package com.example.documentservice.dto;

import com.example.documentservice.domain.Document;

import java.time.Instant;
import java.util.UUID;

public class DocumentResponse {

    private UUID id;
    private String name;
    private String description;
    private String contentType;
    private long sizeBytes;
    private String storageKey;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public static DocumentResponse from(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.id = document.getId();
        response.name = document.getName();
        response.description = document.getDescription();
        response.contentType = document.getContentType();
        response.sizeBytes = document.getSizeBytes();
        response.storageKey = document.getStorageKey();
        response.createdAt = document.getCreatedAt();
        response.updatedAt = document.getUpdatedAt();
        response.version = document.getVersion();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
