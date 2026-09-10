package com.example.documentservice.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Lifecycle event published to ActiveMQ whenever a document is created, updated
 * or deleted.
 */
public class DocumentEvent {

    public enum Type {
        CREATED,
        UPDATED,
        DELETED
    }

    private Type type;
    private UUID documentId;
    private String name;
    private String storageKey;
    private Instant occurredAt;

    public DocumentEvent() {
    }

    public DocumentEvent(Type type, UUID documentId, String name, String storageKey) {
        this.type = type;
        this.documentId = documentId;
        this.name = name;
        this.storageKey = storageKey;
        this.occurredAt = Instant.now();
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
