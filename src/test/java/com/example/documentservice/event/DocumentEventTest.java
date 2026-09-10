package com.example.documentservice.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentEventTest {

    @Test
    void richConstructorPopulatesFieldsAndOccurredAt() {
        UUID id = UUID.randomUUID();
        Instant before = Instant.now();

        DocumentEvent event = new DocumentEvent(
                DocumentEvent.Type.CREATED, id, "name", "storage/key");

        assertThat(event.getType()).isEqualTo(DocumentEvent.Type.CREATED);
        assertThat(event.getDocumentId()).isEqualTo(id);
        assertThat(event.getName()).isEqualTo("name");
        assertThat(event.getStorageKey()).isEqualTo("storage/key");
        assertThat(event.getOccurredAt()).isBetween(before, Instant.now());
    }

    @Test
    void noArgConstructorAndSettersRoundTrip() {
        DocumentEvent event = new DocumentEvent();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        event.setType(DocumentEvent.Type.DELETED);
        event.setDocumentId(id);
        event.setName("n");
        event.setStorageKey("k");
        event.setOccurredAt(now);

        assertThat(event.getType()).isEqualTo(DocumentEvent.Type.DELETED);
        assertThat(event.getDocumentId()).isEqualTo(id);
        assertThat(event.getName()).isEqualTo("n");
        assertThat(event.getStorageKey()).isEqualTo("k");
        assertThat(event.getOccurredAt()).isEqualTo(now);
    }

    @Test
    void typeEnumHasThreeValues() {
        assertThat(DocumentEvent.Type.values())
                .containsExactly(
                        DocumentEvent.Type.CREATED,
                        DocumentEvent.Type.UPDATED,
                        DocumentEvent.Type.DELETED);
        assertThat(DocumentEvent.Type.valueOf("UPDATED")).isEqualTo(DocumentEvent.Type.UPDATED);
    }
}
