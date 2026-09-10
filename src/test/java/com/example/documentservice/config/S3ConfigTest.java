package com.example.documentservice.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    private final S3Config config = new S3Config();

    @Test
    void customEndpointUsesPathStyleAccessAndAppliesStaticCredentials() {
        S3Properties props = new S3Properties();
        props.setEndpoint("http://localhost:4566");
        props.setRegion("us-east-1");
        props.setBucket("documents");
        props.setAccessKey("AKIAENDPOINTTESTKEY");
        props.setSecretKey("endpoint-secret");

        S3Client client = config.s3Client(props);

        // Path-style access: bucket stays in the path, not the host.
        URL url = client.utilities().getUrl(b -> b.bucket("documents").key("documents/a/b.txt"));
        assertThat(url.getHost()).isEqualTo("localhost");
        assertThat(url.getPort()).isEqualTo(4566);
        assertThat(url.getPath()).isEqualTo("/documents/documents/a/b.txt");
        assertThat(client.serviceClientConfiguration().endpointOverride())
                .contains(java.net.URI.create("http://localhost:4566"));
        assertThat(resolvedAccessKey(client)).isEqualTo("AKIAENDPOINTTESTKEY");
    }

    @Test
    void customEndpointWithoutStaticCredentialsStillBuilds() {
        S3Properties props = new S3Properties();
        props.setEndpoint("http://minio.local:9000");
        props.setRegion("us-east-1");

        S3Client client = config.s3Client(props);

        URL url = client.utilities().getUrl(b -> b.bucket("b").key("k"));
        assertThat(url.getHost()).isEqualTo("minio.local");
        assertThat(client.serviceClientConfiguration().credentialsProvider())
                .isNotInstanceOf(StaticCredentialsProvider.class);
    }

    @Test
    void regionModeUsesConfiguredRegionAndAppliesStaticCredentials() {
        S3Properties props = new S3Properties();
        props.setRegion("eu-west-1");
        props.setAccessKey("AKIAREGIONTESTKEY");
        props.setSecretKey("region-secret");

        S3Client client = config.s3Client(props);

        assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.EU_WEST_1);
        assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
        URL url = client.utilities().getUrl(b -> b.bucket("b").key("k"));
        assertThat(url.getHost()).contains("eu-west-1.amazonaws.com");
        assertThat(resolvedAccessKey(client)).isEqualTo("AKIAREGIONTESTKEY");
    }

    @Test
    void regionModeWithoutStaticCredentialsStillBuilds() {
        S3Properties props = new S3Properties();
        props.setRegion("us-east-1");

        S3Client client = config.s3Client(props);

        assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.US_EAST_1);
        assertThat(client.serviceClientConfiguration().credentialsProvider())
                .isNotInstanceOf(StaticCredentialsProvider.class);
    }

    private static String resolvedAccessKey(S3Client client) {
        IdentityProvider<? extends AwsCredentialsIdentity> provider =
                client.serviceClientConfiguration().credentialsProvider();
        assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
        return ((AwsCredentialsProvider) provider).resolveCredentials().accessKeyId();
    }
}
