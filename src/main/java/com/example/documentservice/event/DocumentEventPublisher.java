package com.example.documentservice.event;

import com.example.documentservice.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin ActiveMQ producer for {@link DocumentEvent}s.
 */
@Component
public class DocumentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DocumentEventPublisher.class);

    private final JmsTemplate jmsTemplate;
    private final MessagingProperties properties;

    public DocumentEventPublisher(JmsTemplate jmsTemplate, MessagingProperties properties) {
        this.jmsTemplate = jmsTemplate;
        this.properties = properties;
    }

    public void publish(DocumentEvent event) {
        String queue = properties.getDocumentEventsQueue();
        jmsTemplate.convertAndSend(queue, event);
        log.info("Published {} event for document {} to queue '{}'",
                event.getType(), event.getDocumentId(), queue);
    }
}
