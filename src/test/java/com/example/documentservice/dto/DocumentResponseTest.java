package com.example.documentservice.dto;

import com.example.documentservice.domain.Document;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentResponseTest {

    @Test
    void fromCopiesEveryField() {
        Document doc = new Document();
        UUID id = UUID.randomUUID();
        doc.setId(id);
        doc.setName("name");
        doc.setDescription("description");
        doc.setContentType("application/pdf");
        doc.setSizeBytes(123L);
        doc.setStorageKey("documents/id/name.pdf");
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
        Instant updatedAt = Instant.parse("2026-01-02T03:09:05Z");
        ReflectionTestUtils.setField(doc, "createdAt", createdAt);
        ReflectionTestUtils.setField(doc, "updatedAt", updatedAt);

        DocumentResponse response = DocumentResponse.from(doc);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("name");
        assertThat(response.getDescription()).isEqualTo("description");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getSizeBytes()).isEqualTo(123L);
        assertThat(response.getStorageKey()).isEqualTo("documents/id/name.pdf");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(response.getVersion()).isEqualTo(doc.getVersion());
    }

    @Test
    void fromToleratesNullOptionalFields() {
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setName("only-name");

        DocumentResponse response = DocumentResponse.from(doc);

        assertThat(response.getDescription()).isNull();
        assertThat(response.getContentType()).isNull();
        assertThat(response.getCreatedAt()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
        assertThat(response.getName()).isEqualTo("only-name");
    }
}
