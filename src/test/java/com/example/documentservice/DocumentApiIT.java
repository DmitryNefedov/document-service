package com.example.documentservice;

import com.example.documentservice.event.DocumentEvent;
import com.example.documentservice.repository.DocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end API test. Nothing is mocked in-process:
 *
 * <ul>
 *   <li>PostgreSQL, ActiveMQ and S3 (LocalStack) each run in their own throwaway
 *       Testcontainers container - no docker-compose, no ambassador containers.</li>
 * </ul>
 *
 * <b>DB is never polluted:</b> the PostgreSQL container is created fresh for this
 * class and destroyed afterwards, so no shared/persistent database is touched.
 * On top of that {@link #cleanUp()} clears the {@code documents} table and the S3
 * bucket after every test, so the tests stay independent of one another.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class DocumentApiIT {

    private static final String BUCKET = "documents-it";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("documents")
                    .withUsername("documents")
                    .withPassword("documents");

    @Container
    static final GenericContainer<?> ACTIVEMQ =
            new GenericContainer<>(DockerImageName.parse("apache/activemq-classic:6.1.7"))
                    .withEnv("ACTIVEMQ_CONNECTION_USER", "admin")
                    .withEnv("ACTIVEMQ_CONNECTION_PASSWORD", "admin")
                    .withExposedPorts(61616)
                    .waitingFor(Wait.forListeningPort());

    @Container
    static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.9.1"))
                    .withServices(LocalStackContainer.Service.S3);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.activemq.broker-url", () -> String.format(
                "tcp://%s:%d", ACTIVEMQ.getHost(), ACTIVEMQ.getMappedPort(61616)));
        registry.add("spring.activemq.user", () -> "admin");
        registry.add("spring.activemq.password", () -> "admin");

        registry.add("app.s3.endpoint",
                () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("app.s3.region", LOCALSTACK::getRegion);
        registry.add("app.s3.access-key", LOCALSTACK::getAccessKey);
        registry.add("app.s3.secret-key", LOCALSTACK::getSecretKey);
        registry.add("app.s3.bucket", () -> BUCKET);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private EventRecorder events;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private S3Client s3;

    @AfterEach
    void cleanUp() {
        documentRepository.deleteAll();
        s3.listObjectsV2(b -> b.bucket(BUCKET)).contents()
                .forEach(o -> s3.deleteObject(d -> d.bucket(BUCKET).key(o.key())));
        events.drain();
    }

    @Test
    void fullDocumentCrudLifecycleOverHttp() throws Exception {
        // CREATE
        ResponseEntity<Map<String, Object>> created = rest.exchange(
                "/api/documents", HttpMethod.POST,
                multipart("report.txt", "text/plain", "hello world",
                        Map.of("name", "Quarterly report", "description", "Q1 numbers")),
                mapType());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        String id = (String) created.getBody().get("id");
        assertThat(id).isNotBlank();
        assertThat(created.getBody().get("name")).isEqualTo("Quarterly report");
        assertThat(((Number) created.getBody().get("sizeBytes")).longValue()).isEqualTo(11L);
        assertThat(((Number) created.getBody().get("version")).longValue()).isZero();
        assertThat(events.poll()).satisfies(e -> {
            assertThat(e.getType()).isEqualTo(DocumentEvent.Type.CREATED);
            assertThat(e.getDocumentId()).isEqualTo(UUID.fromString(id));
        });

        // READ (single + list)
        ResponseEntity<Map<String, Object>> fetched = rest.exchange(
                "/api/documents/" + id, HttpMethod.GET, null, mapType());
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().get("description")).isEqualTo("Q1 numbers");

        ResponseEntity<List<Map<String, Object>>> listed = rest.exchange(
                "/api/documents", HttpMethod.GET, null, listType());
        assertThat(listed.getBody()).extracting(m -> m.get("id")).containsExactly(id);

        // DOWNLOAD content from S3
        ResponseEntity<byte[]> content = rest.getForEntity(
                "/api/documents/" + id + "/content", byte[].class);
        assertThat(content.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(content.getBody(), StandardCharsets.UTF_8)).isEqualTo("hello world");

        // UPDATE (metadata + new payload)
        ResponseEntity<Map<String, Object>> updated = rest.exchange(
                "/api/documents/" + id, HttpMethod.PUT,
                multipart("report-v2.txt", "text/plain", "hello world v2",
                        Map.of("name", "Quarterly report (final)")),
                mapType());
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("name")).isEqualTo("Quarterly report (final)");
        assertThat(((Number) updated.getBody().get("version")).longValue()).isGreaterThan(0L);
        assertThat(events.poll().getType()).isEqualTo(DocumentEvent.Type.UPDATED);

        ResponseEntity<byte[]> updatedContent = rest.getForEntity(
                "/api/documents/" + id + "/content", byte[].class);
        assertThat(new String(updatedContent.getBody(), StandardCharsets.UTF_8)).isEqualTo("hello world v2");

        // DELETE
        ResponseEntity<Void> deleted = rest.exchange(
                "/api/documents/" + id, HttpMethod.DELETE, null, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(events.poll().getType()).isEqualTo(DocumentEvent.Type.DELETED);

        ResponseEntity<Map<String, Object>> afterDelete = rest.exchange(
                "/api/documents/" + id, HttpMethod.GET, null, mapType());
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownDocumentReturns404() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/api/documents/" + UUID.randomUUID(), HttpMethod.GET, null, mapType());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("status")).isEqualTo(404);
    }

    @Test
    void badRequestsAreRejected() {
        // missing file part
        ResponseEntity<Map<String, Object>> missingFile = rest.exchange(
                "/api/documents", HttpMethod.POST,
                multipart(null, null, null, Map.of("name", "x")), mapType());
        assertThat(missingFile.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // malformed UUID
        ResponseEntity<Map<String, Object>> badId = rest.exchange(
                "/api/documents/not-a-uuid", HttpMethod.GET, null, mapType());
        assertThat(badId.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static HttpEntity<MultiValueMap<String, Object>> multipart(
            String filename, String contentType, String body, Map<String, String> fields) {

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        if (body != null) {
            ByteArrayResource resource = new ByteArrayResource(body.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(contentType));
            parts.add("file", new HttpEntity<Resource>(resource, fileHeaders));
        }
        fields.forEach(parts::add);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(parts, headers);
    }

    private static ParameterizedTypeReference<Map<String, Object>> mapType() {
        return new ParameterizedTypeReference<Map<String, Object>>() {
        };
    }

    private static ParameterizedTypeReference<List<Map<String, Object>>> listType() {
        return new ParameterizedTypeReference<List<Map<String, Object>>>() {
        };
    }

    @TestConfiguration
    static class RecorderConfig {
        @Bean
        EventRecorder eventRecorder() {
            return new EventRecorder();
        }
    }

    static class EventRecorder {
        private final BlockingQueue<DocumentEvent> queue = new LinkedBlockingQueue<>();

        @JmsListener(destination = "${app.messaging.document-events-queue}")
        void onEvent(DocumentEvent event) {
            queue.add(event);
        }

        DocumentEvent poll() throws InterruptedException {
            DocumentEvent event = queue.poll(10, TimeUnit.SECONDS);
            assertThat(event).as("expected a document event on the queue").isNotNull();
            return event;
        }

        void drain() {
            queue.clear();
        }
    }
}
