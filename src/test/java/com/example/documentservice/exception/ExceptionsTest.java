package com.example.documentservice.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    @Test
    void documentNotFoundBuildsMessageFromId() {
        UUID id = UUID.randomUUID();

        DocumentNotFoundException ex = new DocumentNotFoundException(id);

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Document not found: " + id);
    }

    @Test
    void storageExceptionKeepsMessageAndCause() {
        Throwable cause = new IllegalStateException("boom");

        StorageException ex = new StorageException("wrapper", cause);

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("wrapper");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
