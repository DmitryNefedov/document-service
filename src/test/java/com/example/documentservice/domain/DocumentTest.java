package com.example.documentservice.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest {

    @Test
    void gettersAndSettersRoundTrip() {
        Document doc = new Document();
        UUID id = UUID.randomUUID();

        doc.setId(id);
        doc.setName("report");
        doc.setDescription("desc");
        doc.setContentType("text/plain");
        doc.setSizeBytes(42L);
        doc.setStorageKey("documents/x/report.txt");

        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getName()).isEqualTo("report");
        assertThat(doc.getDescription()).isEqualTo("desc");
        assertThat(doc.getContentType()).isEqualTo("text/plain");
        assertThat(doc.getSizeBytes()).isEqualTo(42L);
        assertThat(doc.getStorageKey()).isEqualTo("documents/x/report.txt");
        assertThat(doc.getVersion()).isZero();
        assertThat(doc.getCreatedAt()).isNull();
        assertThat(doc.getUpdatedAt()).isNull();
    }

    @Test
    void onCreateGeneratesIdWhenMissingAndStampsBothTimes() {
        Document doc = new Document();
        Instant before = Instant.now();

        doc.onCreate();

        assertThat(doc.getId()).isNotNull();
        assertThat(doc.getCreatedAt()).isBetween(before, Instant.now());
        assertThat(doc.getUpdatedAt()).isEqualTo(doc.getCreatedAt());
    }

    @Test
    void onCreateKeepsExistingId() {
        Document doc = new Document();
        UUID id = UUID.randomUUID();
        doc.setId(id);

        doc.onCreate();

        assertThat(doc.getId()).isEqualTo(id);
    }

    @Test
    void onUpdateMovesUpdatedAtForward() throws InterruptedException {
        Document doc = new Document();
        doc.onCreate();
        Instant created = doc.getCreatedAt();
        Thread.sleep(2);

        doc.onUpdate();

        assertThat(doc.getCreatedAt()).isEqualTo(created);
        assertThat(doc.getUpdatedAt()).isAfter(created);
    }
}
