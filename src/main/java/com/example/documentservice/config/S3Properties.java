package com.example.documentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding for {@code app.s3.*}. When {@link #endpoint} is set the client talks to a
 * custom endpoint (LocalStack / MinIO) with path-style access; otherwise it targets
 * real AWS S3 in {@link #region}.
 */
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

    private String endpoint;
    private String region = "us-east-1";
    private String bucket = "documents";
    private String accessKey;
    private String secretKey;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    public boolean hasStaticCredentials() {
        return accessKey != null && !accessKey.isBlank();
    }
}
