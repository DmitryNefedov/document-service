package com.example.documentservice.connector;

import com.example.documentservice.dto.DocumentContent;
import com.example.documentservice.dto.DocumentResponse;
import com.example.documentservice.domain.Document;
import com.example.documentservice.exception.DocumentNotFoundException;
import com.example.documentservice.exception.RestExceptionHandler;
import com.example.documentservice.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentConnectorTest {

    private final DocumentService documentService = mock(DocumentService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DocumentConnector(documentService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private static DocumentResponse response(UUID id, String name) {
        Document doc = new Document();
        doc.setId(id);
        doc.setName(name);
        doc.setContentType("text/plain");
        doc.setSizeBytes(4L);
        doc.setStorageKey("documents/" + id + "/" + name);
        return DocumentResponse.from(doc);
    }

    @Test
    void listReturnsJsonArray() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.list()).thenReturn(List.of(response(id, "a.txt")));

        mvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("a.txt"));
    }

    @Test
    void getReturnsSingleDocument() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.get(id)).thenReturn(response(id, "a.txt"));

        mvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getUnknownDocumentIsMappedTo404() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.get(id)).thenThrow(new DocumentNotFoundException(id));

        mvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void contentUsesStoredContentType() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.download(id)).thenReturn(new DocumentContent(
                "hello".getBytes(StandardCharsets.UTF_8), "text/plain", "a.txt"));

        mvc.perform(get("/api/documents/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"a.txt\""))
                .andExpect(content().bytes("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void contentFallsBackToOctetStreamWhenContentTypeMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.download(id)).thenReturn(new DocumentContent(
                new byte[] {1, 2, 3}, null, "blob.bin"));

        mvc.perform(get("/api/documents/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.create(eq("Title"), eq("Desc"), any())).thenReturn(response(id, "n.txt"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "n.txt", "text/plain", "data".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/documents").file(file).param("name", "Title").param("description", "Desc"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/documents/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void createWithoutOptionalParamsPassesNulls() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.create(isNull(), isNull(), any())).thenReturn(response(id, "n.txt"));

        mvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "n.txt", "text/plain", new byte[] {1})))
                .andExpect(status().isCreated());

        verify(documentService).create(isNull(), isNull(), any());
    }

    @Test
    void updateForwardsMultipartToService() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.update(eq(id), eq("New"), isNull(), any())).thenReturn(response(id, "New"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "v2.txt", "text/plain", "v2".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/documents/{id}", id).file(file).param("name", "New")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"));

        ArgumentCaptor<org.springframework.web.multipart.MultipartFile> captor =
                ArgumentCaptor.forClass(org.springframework.web.multipart.MultipartFile.class);
        verify(documentService).update(eq(id), eq("New"), isNull(), captor.capture());
        assertThat(captor.getValue().getOriginalFilename()).isEqualTo("v2.txt");
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mvc.perform(delete("/api/documents/{id}", id))
                .andExpect(status().isNoContent());

        verify(documentService).delete(id);
    }
}
