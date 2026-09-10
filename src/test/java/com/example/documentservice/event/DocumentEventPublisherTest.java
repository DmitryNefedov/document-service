package com.example.documentservice.event;

import com.example.documentservice.config.MessagingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.core.JmsTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DocumentEventPublisherTest {

    private final JmsTemplate jmsTemplate = mock(JmsTemplate.class);

    @Test
    void publishSendsEventToConfiguredQueue() {
        MessagingProperties properties = new MessagingProperties();
        properties.setDocumentEventsQueue("custom-queue");
        DocumentEventPublisher publisher = new DocumentEventPublisher(jmsTemplate, properties);
        DocumentEvent event = new DocumentEvent(
                DocumentEvent.Type.UPDATED, UUID.randomUUID(), "n", "k");

        publisher.publish(event);

        ArgumentCaptor<String> queue = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(jmsTemplate).convertAndSend(queue.capture(), payload.capture());
        assertThat(queue.getValue()).isEqualTo("custom-queue");
        assertThat(payload.getValue()).isSameAs(event);
    }
}
