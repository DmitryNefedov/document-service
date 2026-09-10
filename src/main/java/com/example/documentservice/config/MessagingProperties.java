package com.example.documentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding for {@code app.messaging.*}: the ActiveMQ destination that document
 * lifecycle events are published to.
 */
@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    private String documentEventsQueue = "document-events";

    public String getDocumentEventsQueue() {
        return documentEventsQueue;
    }

    public void setDocumentEventsQueue(String documentEventsQueue) {
        this.documentEventsQueue = documentEventsQueue;
    }
}
