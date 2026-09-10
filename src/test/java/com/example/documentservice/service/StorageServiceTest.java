package com.example.documentservice.service;

import com.example.documentservice.config.S3Properties;
import com.example.documentservice.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageServiceTest {

    private static final String BUCKET = "documents";

    private final S3Client s3 = mock(S3Client.class);
    private StorageService storage;

    @BeforeEach
    void setUp() {
        S3Properties props = new S3Properties();
        props.setBucket(BUCKET);
        storage = new StorageService(s3, props);
    }

    @Test
    void ensureBucketCreatesBucketWhenMissing() {
        when(s3.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().message("missing").build());

        storage.ensureBucket();

        ArgumentCaptor<CreateBucketRequest> created = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3).createBucket(created.capture());
        assertThat(created.getValue().bucket()).isEqualTo(BUCKET);
    }

    @Test
    void ensureBucketSkipsCreationWhenPresent() {
        when(s3.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        storage.ensureBucket();

        verify(s3, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void ensureBucketWrapsFailures() {
        when(s3.headBucket(any(HeadBucketRequest.class))).thenThrow(new RuntimeException("no s3"));

        assertThatThrownBy(() -> storage.ensureBucket())
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Unable to initialise S3 bucket 'documents'")
                .hasRootCauseMessage("no s3");
    }

    @Test
    void putStoresBytesWithContentTypeAndLength() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        storage.put("key/a.txt", data, "text/plain");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(request.capture(), body.capture());
        assertThat(request.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(request.getValue().key()).isEqualTo("key/a.txt");
        assertThat(request.getValue().contentType()).isEqualTo("text/plain");
        assertThat(body.getValue().optionalContentLength()).contains(5L);
    }

    @Test
    void putOmitsContentTypeWhenNullOrBlank() {
        storage.put("k1", new byte[] {1, 2}, null);
        storage.put("k2", new byte[] {1, 2}, "   ");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3, times(2)).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getAllValues()).allSatisfy(r -> assertThat(r.contentType()).isNull());
    }

    @Test
    void putWrapsFailures() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("write failed"));

        assertThatThrownBy(() -> storage.put("k", new byte[] {0}, "text/plain"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to store object 'k'");
    }

    @Test
    void getReturnsObjectBytes() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(), "payload".getBytes(StandardCharsets.UTF_8)));

        byte[] bytes = storage.get("k");

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("payload");
    }

    @Test
    void getWrapsFailures() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(new RuntimeException("read failed"));

        assertThatThrownBy(() -> storage.get("k"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to read object 'k'");
    }

    @Test
    void deleteRemovesObject() {
        storage.delete("k");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(request.getValue().key()).isEqualTo("k");
    }

    @Test
    void deleteWrapsFailures() {
        when(s3.deleteObject(any(DeleteObjectRequest.class))).thenThrow(new RuntimeException("delete failed"));

        assertThatThrownBy(() -> storage.delete("k"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to delete object 'k'");
    }
}
