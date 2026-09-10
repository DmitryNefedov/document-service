package com.example.documentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serialize JMS payloads as JSON text messages so the document events are
 * broker- and language-neutral (no Java serialization on the wire).
 */
@Configuration
public class JmsConfig {

    /**
     * Reuse Spring Boot's auto-configured Jackson 3 {@link JsonMapper} so the
     * converter serializes {@code java.time} fields such as
     * {@code DocumentEvent.occurredAt} with the application's configuration.
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}
