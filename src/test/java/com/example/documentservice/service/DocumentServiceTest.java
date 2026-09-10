package com.example.documentservice.service;

import com.example.documentservice.domain.Document;
import com.example.documentservice.dto.DocumentContent;
import com.example.documentservice.dto.DocumentResponse;
import com.example.documentservice.event.DocumentEvent;
import com.example.documentservice.event.DocumentEventPublisher;
import com.example.documentservice.exception.DocumentNotFoundException;
import com.example.documentservice.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private final DocumentRepository repository = mock(DocumentRepository.class);
    private final StorageService storage = mock(StorageService.class);
    private final DocumentEventPublisher events = mock(DocumentEventPublisher.class);
    private final DocumentService service = new DocumentService(repository, storage, events);

    private static Document persisted(UUID id, String name, String key) {
        Document doc = new Document();
        doc.setId(id);
        doc.setName(name);
        doc.setStorageKey(key);
        doc.setContentType("text/plain");
        doc.setSizeBytes(3L);
        return doc;
    }

    // ---- list / get / download -------------------------------------------------

    @Test
    void listMapsEveryEntity() {
        Document a = persisted(UUID.randomUUID(), "a", "k/a");
        Document b = persisted(UUID.randomUUID(), "b", "k/b");
        when(repository.findAll()).thenReturn(List.of(a, b));

        List<DocumentResponse> result = service.list();

        assertThat(result).extracting(DocumentResponse::getName).containsExactly("a", "b");
    }

    @Test
    void getReturnsMappedDocument() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(persisted(id, "n", "k")));

        assertThat(service.get(id).getId()).isEqualTo(id);
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void downloadPullsPayloadFromStorage() {
        UUID id = UUID.randomUUID();
        Document doc = persisted(id, "file.txt", "documents/x/file.txt");
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(storage.get("documents/x/file.txt")).thenReturn("bytes".getBytes(StandardCharsets.UTF_8));

        DocumentContent content = service.download(id);

        assertThat(new String(content.getData(), StandardCharsets.UTF_8)).isEqualTo("bytes");
        assertThat(content.getContentType()).isEqualTo("text/plain");
        assertThat(content.getFilename()).isEqualTo("file.txt");
    }

    @Test
    void downloadThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(id)).isInstanceOf(DocumentNotFoundException.class);
        verifyNoInteractions(storage);
    }

    // ---- create --------------------------------------------------------------

    @Test
    void createUsesProvidedNameStoresPayloadAndPublishesEvent() {
        when(repository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));

        DocumentResponse response = service.create("My name", "desc", file);

        assertThat(response.getName()).isEqualTo("My name");
        assertThat(response.getDescription()).isEqualTo("desc");
        assertThat(response.getContentType()).isEqualTo("text/plain");
        assertThat(response.getSizeBytes()).isEqualTo(3L);
        assertThat(response.getStorageKey())
                .isEqualTo("documents/" + response.getId() + "/notes.txt");

        verify(storage).put(response.getStorageKey(), "abc".getBytes(StandardCharsets.UTF_8), "text/plain");
        DocumentEvent event = capturePublished();
        assertThat(event.getType()).isEqualTo(DocumentEvent.Type.CREATED);
        assertThat(event.getDocumentId()).isEqualTo(response.getId());
    }

    @Test
    void createFallsBackToOriginalFilenameWhenNameBlank() {
        when(repository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", new byte[] {1});

        DocumentResponse response = service.create("  ", null, file);

        assertThat(response.getName()).isEqualTo("report.pdf");
        assertThat(response.getDescription()).isNull();
    }

    @Test
    void createFallsBackToUntitledAndPayloadKeyWhenFilenameIsNull() throws IOException {
        when(repository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getContentType()).thenReturn("application/octet-stream");
        when(file.getSize()).thenReturn(2L);
        when(file.getBytes()).thenReturn(new byte[] {1, 2});

        DocumentResponse response = service.create(null, null, file);

        assertThat(response.getName()).isEqualTo("untitled");
        assertThat(response.getStorageKey())
                .isEqualTo("documents/" + response.getId() + "/payload");
    }

    @Test
    void createWrapsIoErrorReadingUpload() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("x.txt");
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getBytes()).thenThrow(new IOException("stream broke"));

        assertThatThrownBy(() -> service.create("n", "d", file))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(IOException.class);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    // ---- update ------------------------------------------------------------

    @Test
    void updateThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, "n", "d", null))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void updateMetadataOnlyLeavesStorageUntouched() {
        UUID id = UUID.randomUUID();
        Document doc = persisted(id, "old", "documents/x/old.txt");
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(repository.saveAndFlush(doc)).thenReturn(doc);

        DocumentResponse response = service.update(id, "new name", "new desc", null);

        assertThat(response.getName()).isEqualTo("new name");
        assertThat(response.getDescription()).isEqualTo("new desc");
        assertThat(response.getStorageKey()).isEqualTo("documents/x/old.txt");
        verify(storage, never()).delete(anyString());
        verify(storage, never()).put(anyString(), any(), any());
        assertThat(capturePublished().getType()).isEqualTo(DocumentEvent.Type.UPDATED);
    }

    @Test
    void updateIgnoresBlankNameButAcceptsEmptyDescription() {
        UUID id = UUID.randomUUID();
        Document doc = persisted(id, "keep", "k");
        doc.setDescription("original");
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(repository.saveAndFlush(doc)).thenReturn(doc);

        DocumentResponse response = service.update(id, "   ", "", new MockMultipartFile("file", new byte[0]));

        assertThat(response.getName()).isEqualTo("keep");
        assertThat(response.getDescription()).isEmpty();
    }

    @Test
    void updateWithNullDescriptionKeepsExisting() {
        UUID id = UUID.randomUUID();
        Document doc = persisted(id, "keep", "k");
        doc.setDescription("original");
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(repository.saveAndFlush(doc)).thenReturn(doc);

        DocumentResponse response = service.update(id, null, null, null);

        assertThat(response.getDescription()).isEqualTo("original");
    }

    @Test
    void updateWithNewFileReplacesPayload() {
        UUID id = UUID.randomUUID();
        Document doc = persisted(id, "old", "documents/x/old.txt");
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(repository.saveAndFlush(doc)).thenReturn(doc);
        MockMultipartFile file = new MockMultipartFile(
                "file", "v2.txt", "text/markdown", "v2".getBytes(StandardCharsets.UTF_8));

        DocumentResponse response = service.update(id, null, null, file);

        verify(storage).delete("documents/x/old.txt");
        String newKey = "documents/" + id + "/v2.txt";
        verify(storage).put(newKey, "v2".getBytes(StandardCharsets.UTF_8), "text/markdown");
        assertThat(response.getStorageKey()).isEqualTo(newKey);
        assertThat(response.getContentType()).isEqualTo("text/markdown");
        assertThat(response.getSizeBytes()).isEqualTo(2L);
    }

    // ---- delete ----------------------------------------------------------

    @Test
    void deleteRemovesPayloadRowAndPublishesEvent() {
        UUID id = UUID.randomUUID();
        Document doc = persisted(id, "n", "documents/x/n.txt");
        when(repository.findById(id)).thenReturn(Optional.of(doc));

        service.delete(id);

        verify(storage).delete("documents/x/n.txt");
        verify(repository).delete(doc);
        DocumentEvent event = capturePublished();
        assertThat(event.getType()).isEqualTo(DocumentEvent.Type.DELETED);
        assertThat(event.getDocumentId()).isEqualTo(id);
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(DocumentNotFoundException.class);
        verifyNoInteractions(storage);
        verify(repository, never()).delete(any());
    }

    private DocumentEvent capturePublished() {
        ArgumentCaptor<DocumentEvent> captor = ArgumentCaptor.forClass(DocumentEvent.class);
        verify(events, times(1)).publish(captor.capture());
        return captor.getValue();
    }
}
