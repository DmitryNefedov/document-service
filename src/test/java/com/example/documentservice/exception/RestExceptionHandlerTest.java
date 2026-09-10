package com.example.documentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void notFoundMapsTo404() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new DocumentNotFoundException(id));

        assertBody(response, HttpStatus.NOT_FOUND, "Document not found: " + id);
    }

    @Test
    void validationErrorMapsTo400() {
        ResponseEntity<Map<String, Object>> response = handler.handleValidation(
                new MissingServletRequestParameterException("file", "MultipartFile"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
    }

    @Test
    void multipartErrorMapsTo400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMultipart(new MultipartException("no multipart boundary"));

        assertBody(response, HttpStatus.BAD_REQUEST, "no multipart boundary");
    }

    @Test
    void storageErrorMapsTo502() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleStorage(new StorageException("s3 down", new RuntimeException()));

        assertBody(response, HttpStatus.BAD_GATEWAY, "s3 down");
    }

    private static void assertBody(ResponseEntity<Map<String, Object>> response,
                                   HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody())
                .containsEntry("status", status.value())
                .containsEntry("error", status.getReasonPhrase())
                .containsEntry("message", message)
                .containsKey("timestamp");
    }
}
