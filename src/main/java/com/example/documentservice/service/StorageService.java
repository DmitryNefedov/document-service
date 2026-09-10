package com.example.documentservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.example.documentservice.config.S3Properties;
import com.example.documentservice.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Object-storage repository. Wraps the AWS SDK v1 {@link AmazonS3} client and
 * owns the lifecycle of the configured bucket.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final AmazonS3 s3;
    private final String bucket;

    public StorageService(AmazonS3 s3, S3Properties properties) {
        this.s3 = s3;
        this.bucket = properties.getBucket();
    }

    @PostConstruct
    void ensureBucket() {
        try {
            if (!s3.doesBucketExistV2(bucket)) {
                s3.createBucket(bucket);
                log.info("Created S3 bucket '{}'", bucket);
            }
        } catch (RuntimeException ex) {
            throw new StorageException("Unable to initialise S3 bucket '" + bucket + "'", ex);
        }
    }

    public void put(String key, byte[] data, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        if (contentType != null && !contentType.isBlank()) {
            metadata.setContentType(contentType);
        }
        try (InputStream in = new ByteArrayInputStream(data)) {
            s3.putObject(bucket, key, in, metadata);
        } catch (IOException | RuntimeException ex) {
            throw new StorageException("Failed to store object '" + key + "'", ex);
        }
    }

    public byte[] get(String key) {
        try (S3Object object = s3.getObject(bucket, key)) {
            return StreamUtils.copyToByteArray(object.getObjectContent());
        } catch (IOException | RuntimeException ex) {
            throw new StorageException("Failed to read object '" + key + "'", ex);
        }
    }

    public void delete(String key) {
        try {
            s3.deleteObject(bucket, key);
        } catch (RuntimeException ex) {
            throw new StorageException("Failed to delete object '" + key + "'", ex);
        }
    }
}
