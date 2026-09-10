package com.example.documentservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * Serialize JMS payloads as JSON text messages so the document events are
 * broker- and language-neutral (no Java serialization on the wire).
 */
@Configuration
public class JmsConfig {

    /**
     * Reuse Spring Boot's auto-configured {@link ObjectMapper} so the converter
     * inherits the JSR-310 module and can serialize {@code Instant} fields such as
     * {@code DocumentEvent.occurredAt}; a bare {@code MappingJackson2MessageConverter}
     * builds its own module-less mapper and fails on Java 8 date/time types.
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
