package com.example.documentservice.config;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    private final S3Config config = new S3Config();

    @Test
    void customEndpointUsesPathStyleAccess() {
        S3Properties props = new S3Properties();
        props.setEndpoint("http://localhost:4566");
        props.setRegion("us-east-1");
        props.setBucket("documents");
        props.setAccessKey("test");
        props.setSecretKey("test");

        AmazonS3 client = config.amazonS3(props);

        URL url = client.getUrl("documents", "documents/a/b.txt");
        assertThat(url.getHost()).isEqualTo("localhost");
        assertThat(url.getPort()).isEqualTo(4566);
        assertThat(url.getPath()).isEqualTo("/documents/documents/a/b.txt");
    }

    @Test
    void customEndpointWithoutStaticCredentialsStillBuilds() {
        S3Properties props = new S3Properties();
        props.setEndpoint("http://minio.local:9000");
        props.setRegion("us-east-1");

        AmazonS3 client = config.amazonS3(props);

        assertThat(client.getUrl("b", "k").getHost()).isEqualTo("minio.local");
    }

    @Test
    void regionModeUsesVirtualHostedStyleAndConfiguredRegion() {
        S3Properties props = new S3Properties();
        props.setRegion("eu-west-1");
        props.setAccessKey("ak");
        props.setSecretKey("sk");

        AmazonS3 client = config.amazonS3(props);

        assertThat(client.getRegionName()).isEqualTo("eu-west-1");
        assertThat(client.getUrl("b", "k").getHost()).contains("eu-west-1.amazonaws.com");
    }

    @Test
    void regionModeWithoutStaticCredentialsStillBuilds() {
        S3Properties props = new S3Properties();
        props.setRegion("us-east-1");

        AmazonS3 client = config.amazonS3(props);

        assertThat(client.getRegionName()).isEqualTo("us-east-1");
    }
}
