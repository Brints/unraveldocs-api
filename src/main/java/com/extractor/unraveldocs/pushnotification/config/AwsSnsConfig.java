package com.extractor.unraveldocs.pushnotification.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Configuration for AWS SNS push notifications.
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws.sns")
@ConditionalOnProperty(name = "aws.sns.enabled", havingValue = "true", matchIfMissing = false)
public class AwsSnsConfig {

    private boolean enabled = false;
    private String platformApplicationArn;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;

    @Bean(name = "snsPushClient")
    @ConditionalOnProperty(name = "aws.sns.enabled", havingValue = "true")
    public SnsClient snsClient() {
        log.info("Initializing AWS SNS client for push notifications");

        // Use explicit credentials if provided, otherwise use default credential chain
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            return SnsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }

        // Use default credential chain
        return SnsClient.builder()
                .region(Region.of(region))
                .build();
    }
}
