package com.example.documentservice.service;

import com.example.documentservice.config.S3Properties;
import com.example.documentservice.exception.StorageException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Object-storage repository. Wraps the AWS SDK v2 {@link S3Client} and owns the
 * lifecycle of the configured bucket.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3;
    private final String bucket;

    public StorageService(S3Client s3, S3Properties properties) {
        this.s3 = s3;
        this.bucket = properties.getBucket();
    }

    @PostConstruct
    void ensureBucket() {
        try {
            if (!bucketExists()) {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created S3 bucket '{}'", bucket);
            }
        } catch (RuntimeException ex) {
            throw new StorageException("Unable to initialise S3 bucket '" + bucket + "'", ex);
        }
    }

    private boolean bucketExists() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (NoSuchBucketException ex) {
            return false;
        }
    }

    public void put(String key, byte[] data, String contentType) {
        PutObjectRequest.Builder request = PutObjectRequest.builder().bucket(bucket).key(key);
        if (contentType != null && !contentType.isBlank()) {
            request.contentType(contentType);
        }
        try {
            s3.putObject(request.build(), RequestBody.fromBytes(data));
        } catch (RuntimeException ex) {
            throw new StorageException("Failed to store object '" + key + "'", ex);
        }
    }

    public byte[] get(String key) {
        try {
            ResponseBytes<GetObjectResponse> object = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return object.asByteArray();
        } catch (RuntimeException ex) {
            throw new StorageException("Failed to read object '" + key + "'", ex);
        }
    }

    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (RuntimeException ex) {
            throw new StorageException("Failed to delete object '" + key + "'", ex);
        }
    }
}
