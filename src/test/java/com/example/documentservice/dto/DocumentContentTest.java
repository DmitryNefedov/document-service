package com.example.documentservice.dto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentContentTest {

    @Test
    void exposesConstructorArguments() {
        byte[] data = "payload".getBytes(StandardCharsets.UTF_8);

        DocumentContent content = new DocumentContent(data, "text/plain", "file.txt");

        assertThat(content.getData()).isEqualTo(data);
        assertThat(content.getContentType()).isEqualTo("text/plain");
        assertThat(content.getFilename()).isEqualTo("file.txt");
    }

    @Test
    void allowsNullContentType() {
        DocumentContent content = new DocumentContent(new byte[0], null, "f");

        assertThat(content.getContentType()).isNull();
        assertThat(content.getData()).isEmpty();
        assertThat(content.getFilename()).isEqualTo("f");
    }
}
