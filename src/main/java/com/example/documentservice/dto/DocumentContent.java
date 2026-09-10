package com.example.documentservice.dto;

/**
 * Binary payload plus the metadata a controller needs to stream it back.
 */
public class DocumentContent {

    private final byte[] data;
    private final String contentType;
    private final String filename;

    public DocumentContent(byte[] data, String contentType, String filename) {
        this.data = data;
        this.contentType = contentType;
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }
}
