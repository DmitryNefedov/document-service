package com.example.documentservice.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the AWS SDK v1 {@link AmazonS3} client from {@link S3Properties}.
 */
@Configuration
public class S3Config {

    @Bean
    public AmazonS3 amazonS3(S3Properties props) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

        if (props.hasCustomEndpoint()) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(props.getEndpoint(), props.getRegion()));
            builder.withPathStyleAccessEnabled(true);
        } else {
            builder.withRegion(props.getRegion());
        }

        if (props.hasStaticCredentials()) {
            builder.withCredentials(new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(props.getAccessKey(), props.getSecretKey())));
        }

        return builder.build();
    }
}
