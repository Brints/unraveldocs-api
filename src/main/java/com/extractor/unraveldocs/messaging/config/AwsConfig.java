package com.extractor.unraveldocs.messaging.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

//import java.net.URI;


@Configuration
public class AwsConfig {
    @Value("${aws.s3.region}")
    private String awsRegion;

    @Value("${aws.access-key}")
    private String awsAccessKey;

    @Value("${aws.secret-key}")
    private String awsSecretKey;

    @Getter
    @Value("${aws.from-email}")
    private String awsFromEmail;

//    @Value("${aws.s3.endpoint}")
//    private String s3Endpoint;

    @Bean
    public S3Client s3Client() {
        AwsCredentialsProvider awsCredentialsProvider = () -> AwsBasicCredentials.create(awsAccessKey, awsSecretKey);

        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(awsRegion));

        // If an endpoint is provided, set it on the S3 client builder
//        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
//            s3ClientBuilder.endpointOverride(URI.create(s3Endpoint)).forcePathStyle(true);
//        }

        return s3ClientBuilder.build();
    }

    @Bean
    public SesClient sesClient() {
        StaticCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsAccessKey, awsSecretKey));

        return SesClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        StaticCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsAccessKey, awsSecretKey));

        return SnsClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(awsRegion))
                .build();
    }

}
