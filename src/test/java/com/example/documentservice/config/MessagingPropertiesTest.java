package com.example.documentservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingPropertiesTest {

    @Test
    void defaultsToDocumentEventsQueue() {
        assertThat(new MessagingProperties().getDocumentEventsQueue()).isEqualTo("document-events");
    }

    @Test
    void queueIsConfigurable() {
        MessagingProperties props = new MessagingProperties();

        props.setDocumentEventsQueue("other-queue");

        assertThat(props.getDocumentEventsQueue()).isEqualTo("other-queue");
    }
}
