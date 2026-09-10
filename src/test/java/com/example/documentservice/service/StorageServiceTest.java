package com.example.documentservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.example.documentservice.config.S3Properties;
import com.example.documentservice.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageServiceTest {

    private static final String BUCKET = "documents";

    private final AmazonS3 s3 = mock(AmazonS3.class);
    private StorageService storage;

    @BeforeEach
    void setUp() {
        S3Properties props = new S3Properties();
        props.setBucket(BUCKET);
        storage = new StorageService(s3, props);
    }

    @Test
    void ensureBucketCreatesBucketWhenMissing() {
        when(s3.doesBucketExistV2(BUCKET)).thenReturn(false);

        storage.ensureBucket();

        verify(s3).createBucket(BUCKET);
    }

    @Test
    void ensureBucketSkipsCreationWhenPresent() {
        when(s3.doesBucketExistV2(BUCKET)).thenReturn(true);

        storage.ensureBucket();

        verify(s3, never()).createBucket(BUCKET);
    }

    @Test
    void ensureBucketWrapsFailures() {
        when(s3.doesBucketExistV2(BUCKET)).thenThrow(new RuntimeException("no s3"));

        assertThatThrownBy(() -> storage.ensureBucket())
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Unable to initialise S3 bucket 'documents'")
                .hasRootCauseMessage("no s3");
    }

    @Test
    void putStoresBytesWithContentTypeAndLength() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        storage.put("key/a.txt", data, "text/plain");

        ArgumentCaptor<ObjectMetadata> meta = ArgumentCaptor.forClass(ObjectMetadata.class);
        verify(s3).putObject(eq(BUCKET), eq("key/a.txt"), any(InputStream.class), meta.capture());
        assertThat(meta.getValue().getContentLength()).isEqualTo(5L);
        assertThat(meta.getValue().getContentType()).isEqualTo("text/plain");
    }

    @Test
    void putOmitsContentTypeWhenNullOrBlank() {
        storage.put("k1", new byte[] {1, 2}, null);
        storage.put("k2", new byte[] {1, 2}, "   ");

        ArgumentCaptor<ObjectMetadata> meta = ArgumentCaptor.forClass(ObjectMetadata.class);
        verify(s3, org.mockito.Mockito.times(2))
                .putObject(eq(BUCKET), any(), any(InputStream.class), meta.capture());
        assertThat(meta.getAllValues()).allSatisfy(m -> assertThat(m.getContentType()).isNull());
    }

    @Test
    void putWrapsFailures() {
        when(s3.putObject(eq(BUCKET), eq("k"), any(InputStream.class), any()))
                .thenThrow(new RuntimeException("write failed"));

        assertThatThrownBy(() -> storage.put("k", new byte[] {0}, "text/plain"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to store object 'k'");
    }

    @Test
    void getReturnsObjectBytes() throws Exception {
        S3Object object = new S3Object();
        object.setObjectContent(new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8)));
        when(s3.getObject(BUCKET, "k")).thenReturn(object);

        byte[] bytes = storage.get("k");

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("payload");
    }

    @Test
    void getWrapsFailures() {
        when(s3.getObject(BUCKET, "k")).thenThrow(new RuntimeException("read failed"));

        assertThatThrownBy(() -> storage.get("k"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to read object 'k'");
    }

    @Test
    void deleteRemovesObject() {
        storage.delete("k");

        verify(s3).deleteObject(BUCKET, "k");
    }

    @Test
    void deleteWrapsFailures() {
        org.mockito.Mockito.doThrow(new RuntimeException("delete failed"))
                .when(s3).deleteObject(BUCKET, "k");

        assertThatThrownBy(() -> storage.delete("k"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to delete object 'k'");
    }
}
