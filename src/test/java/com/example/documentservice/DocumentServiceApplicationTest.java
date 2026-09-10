package com.example.documentservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class DocumentServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            DocumentServiceApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(DocumentServiceApplication.class, args));
        }
    }

    @Test
    void applicationClassIsInstantiable() {
        assertThat(new DocumentServiceApplication()).isNotNull();
    }
}
