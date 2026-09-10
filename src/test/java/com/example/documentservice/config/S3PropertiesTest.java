package com.example.documentservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3PropertiesTest {

    @Test
    void defaults() {
        S3Properties props = new S3Properties();

        assertThat(props.getEndpoint()).isNull();
        assertThat(props.getRegion()).isEqualTo("us-east-1");
        assertThat(props.getBucket()).isEqualTo("documents");
        assertThat(props.getAccessKey()).isNull();
        assertThat(props.getSecretKey()).isNull();
        assertThat(props.hasCustomEndpoint()).isFalse();
        assertThat(props.hasStaticCredentials()).isFalse();
    }

    @Test
    void settersRoundTrip() {
        S3Properties props = new S3Properties();
        props.setEndpoint("http://localhost:4566");
        props.setRegion("eu-west-1");
        props.setBucket("bucket");
        props.setAccessKey("ak");
        props.setSecretKey("sk");

        assertThat(props.getEndpoint()).isEqualTo("http://localhost:4566");
        assertThat(props.getRegion()).isEqualTo("eu-west-1");
        assertThat(props.getBucket()).isEqualTo("bucket");
        assertThat(props.getAccessKey()).isEqualTo("ak");
        assertThat(props.getSecretKey()).isEqualTo("sk");
    }

    @Test
    void hasCustomEndpointIsFalseForNullOrBlank() {
        S3Properties props = new S3Properties();
        assertThat(props.hasCustomEndpoint()).isFalse();

        props.setEndpoint("   ");
        assertThat(props.hasCustomEndpoint()).isFalse();

        props.setEndpoint("http://s3.local");
        assertThat(props.hasCustomEndpoint()).isTrue();
    }

    @Test
    void hasStaticCredentialsIsFalseForNullOrBlank() {
        S3Properties props = new S3Properties();
        assertThat(props.hasStaticCredentials()).isFalse();

        props.setAccessKey(" ");
        assertThat(props.hasStaticCredentials()).isFalse();

        props.setAccessKey("AKIA...");
        assertThat(props.hasStaticCredentials()).isTrue();
    }
}
