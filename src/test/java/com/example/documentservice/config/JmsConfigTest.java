package com.example.documentservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JmsConfigTest {

    @Test
    void converterIsJsonTextAndReusesProvidedObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        MessageConverter converter = new JmsConfig().jacksonJmsMessageConverter(objectMapper);

        assertThat(converter).isInstanceOf(MappingJackson2MessageConverter.class);
        assertThat(ReflectionTestUtils.getField(converter, "objectMapper")).isSameAs(objectMapper);
        assertThat(ReflectionTestUtils.getField(converter, "targetType")).isEqualTo(MessageType.TEXT);
        assertThat(ReflectionTestUtils.getField(converter, "typeIdPropertyName")).isEqualTo("_type");
    }
}
