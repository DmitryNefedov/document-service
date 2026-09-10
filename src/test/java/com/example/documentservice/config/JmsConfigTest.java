package com.example.documentservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JmsConfigTest {

    @Test
    void converterIsJsonTextAndReusesProvidedJsonMapper() {
        JsonMapper jsonMapper = JsonMapper.builder().build();

        MessageConverter converter = new JmsConfig().jacksonJmsMessageConverter(jsonMapper);

        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
        assertThat(ReflectionTestUtils.getField(converter, "mapper")).isSameAs(jsonMapper);
        assertThat(ReflectionTestUtils.getField(converter, "targetType")).isEqualTo(MessageType.TEXT);
        assertThat(ReflectionTestUtils.getField(converter, "typeIdPropertyName")).isEqualTo("_type");
    }
}
