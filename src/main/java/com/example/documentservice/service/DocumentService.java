package com.example.documentservice.service;

import com.example.documentservice.domain.Document;
import com.example.documentservice.dto.DocumentContent;
import com.example.documentservice.dto.DocumentResponse;
import com.example.documentservice.event.DocumentEvent;
import com.example.documentservice.event.DocumentEventPublisher;
import com.example.documentservice.exception.DocumentNotFoundException;
import com.example.documentservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Coordinates the three backends for a document: metadata in PostgreSQL (via
 * {@link DocumentRepository}), payload in S3 (via {@link StorageService}) and a
 * lifecycle event on ActiveMQ (via {@link DocumentEventPublisher}).
 */
@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final StorageService storage;
    private final DocumentEventPublisher events;

    public DocumentService(DocumentRepository repository,
                           StorageService storage,
                           DocumentEventPublisher events) {
        this.repository = repository;
        this.storage = storage;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list() {
        return repository.findAll().stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID id) {
        return DocumentResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public DocumentContent download(UUID id) {
        Document document = require(id);
        byte[] data = storage.get(document.getStorageKey());
        return new DocumentContent(data, document.getContentType(), document.getName());
    }

    @Transactional
    public DocumentResponse create(String name, String description, MultipartFile file) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setName(resolveName(name, file));
        document.setDescription(description);
        document.setContentType(file.getContentType());
        document.setSizeBytes(file.getSize());

        String key = storageKey(document.getId(), file);
        document.setStorageKey(key);
        storage.put(key, readBytes(file), file.getContentType());

        // Flush so the response and event carry the persisted state (version,
        // @PrePersist timestamps) and a DB failure surfaces before we publish.
        Document saved = repository.saveAndFlush(document);
        events.publish(new DocumentEvent(DocumentEvent.Type.CREATED,
                saved.getId(), saved.getName(), saved.getStorageKey()));
        return DocumentResponse.from(saved);
    }

    @Transactional
    public DocumentResponse update(UUID id, String name, String description, MultipartFile file) {
        Document document = require(id);

        if (StringUtils.hasText(name)) {
            document.setName(name);
        }
        if (description != null) {
            document.setDescription(description);
        }
        if (file != null && !file.isEmpty()) {
            storage.delete(document.getStorageKey());
            String key = storageKey(document.getId(), file);
            document.setStorageKey(key);
            document.setContentType(file.getContentType());
            document.setSizeBytes(file.getSize());
            storage.put(key, readBytes(file), file.getContentType());
        }

        // Flush so @PreUpdate (updatedAt) and the @Version bump are applied before
        // we build the response and event; a bare save() defers them to commit and
        // the caller sees a stale version/updatedAt.
        Document saved = repository.saveAndFlush(document);
        events.publish(new DocumentEvent(DocumentEvent.Type.UPDATED,
                saved.getId(), saved.getName(), saved.getStorageKey()));
        return DocumentResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Document document = require(id);
        storage.delete(document.getStorageKey());
        repository.delete(document);
        events.publish(new DocumentEvent(DocumentEvent.Type.DELETED,
                document.getId(), document.getName(), document.getStorageKey()));
    }

    private Document require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    private static String resolveName(String name, MultipartFile file) {
        if (StringUtils.hasText(name)) {
            return name;
        }
        String original = file.getOriginalFilename();
        return StringUtils.hasText(original) ? original : "untitled";
    }

    private static String storageKey(UUID id, MultipartFile file) {
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "payload" : file.getOriginalFilename());
        return "documents/" + id + "/" + original;
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to read uploaded file", ex);
        }
    }
}
