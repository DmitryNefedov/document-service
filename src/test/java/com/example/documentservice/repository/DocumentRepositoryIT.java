package com.example.documentservice.repository;

import com.example.documentservice.domain.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence slice test against a real PostgreSQL (Testcontainers).
 *
 * <b>DB is never polluted:</b> the container is throwaway, and {@link DataJpaTest}
 * wraps every test in a transaction that is rolled back afterwards, so not even
 * the ephemeral database keeps any rows between tests.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DocumentRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void prePersistStampsTimestampsAndVersionStartsAtZero() {
        Document doc = newDocument();

        Document saved = repository.saveAndFlush(doc);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void updateBumpsVersionAndUpdatedAt() throws InterruptedException {
        Document saved = repository.saveAndFlush(newDocument());
        entityManager.clear();
        Thread.sleep(5);

        Document reloaded = repository.findById(saved.getId()).orElseThrow();
        reloaded.setDescription("changed");
        Document updated = repository.saveAndFlush(reloaded);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    private static Document newDocument() {
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setName("name");
        doc.setContentType("text/plain");
        doc.setSizeBytes(3L);
        doc.setStorageKey("documents/x/name.txt");
        return doc;
    }
}
