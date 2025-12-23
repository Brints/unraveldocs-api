package com.extractor.unraveldocs.brokers.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic declarations.
 * Topics are auto-created if they don't exist.
 */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaTopicConfig {
    
    // Topic names as constants for reuse
    public static final String TOPIC_EMAILS = "unraveldocs-emails";
    public static final String TOPIC_DOCUMENTS = "unraveldocs-documents";
    public static final String TOPIC_PAYMENTS = "unraveldocs-payments";
    public static final String TOPIC_USERS = "unraveldocs-users";
    public static final String TOPIC_NOTIFICATIONS = "unraveldocs-notifications";
    public static final String TOPIC_RECEIPTS = "unraveldocs-receipts";

    // Retry topics (intermediate retry before DLQ)
    public static final String TOPIC_EMAILS_RETRY = "unraveldocs-emails-retry";
    public static final String TOPIC_DOCUMENTS_RETRY = "unraveldocs-documents-retry";
    public static final String TOPIC_PAYMENTS_RETRY = "unraveldocs-payments-retry";
    public static final String TOPIC_USERS_RETRY = "unraveldocs-users-retry";
    public static final String TOPIC_RECEIPTS_RETRY = "unraveldocs-receipts-retry";

    // Dead Letter Queue topics
    public static final String TOPIC_EMAILS_DLQ = "unraveldocs-emails-dlq";
    public static final String TOPIC_DOCUMENTS_DLQ = "unraveldocs-documents-dlq";
    public static final String TOPIC_PAYMENTS_DLQ = "unraveldocs-payments-dlq";
    public static final String TOPIC_USERS_DLQ = "unraveldocs-users-dlq";
    public static final String TOPIC_RECEIPTS_DLQ = "unraveldocs-receipts-dlq";

    @Bean
    public NewTopic emailsTopic() {
        return TopicBuilder.name(TOPIC_EMAILS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .build();
    }
    
    @Bean
    public NewTopic documentsTopic() {
        return TopicBuilder.name(TOPIC_DOCUMENTS)
                .partitions(6) // Higher partition count for document processing
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }
    
    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name(TOPIC_PAYMENTS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days for payments
                .build();
    }
    
    @Bean
    public NewTopic usersTopic() {
        return TopicBuilder.name(TOPIC_USERS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }
    
    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "259200000") // 3 days
                .build();
    }

    @Bean
    public NewTopic receiptsTopic() {
        return TopicBuilder.name(TOPIC_RECEIPTS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days for receipts
                .build();
    }

    // Dead Letter Queue topics
    @Bean
    public NewTopic emailsDlqTopic() {
        return TopicBuilder.name(TOPIC_EMAILS_DLQ)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .build();
    }
    
    @Bean
    public NewTopic documentsDlqTopic() {
        return TopicBuilder.name(TOPIC_DOCUMENTS_DLQ)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000")
                .build();
    }
    
    @Bean
    public NewTopic paymentsDlqTopic() {
        return TopicBuilder.name(TOPIC_PAYMENTS_DLQ)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000")
                .build();
    }

    @Bean
    public NewTopic usersDlqTopic() {
        return TopicBuilder.name(TOPIC_USERS_DLQ)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000")
                .build();
    }

    @Bean
    public NewTopic receiptsDlqTopic() {
        return TopicBuilder.name(TOPIC_RECEIPTS_DLQ)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000")
                .build();
    }

    // Retry topics (short retention, for intermediate retry attempts)
    @Bean
    public NewTopic emailsRetryTopic() {
        return TopicBuilder.name(TOPIC_EMAILS_RETRY)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "86400000") // 1 day
                .build();
    }

    @Bean
    public NewTopic documentsRetryTopic() {
        return TopicBuilder.name(TOPIC_DOCUMENTS_RETRY)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "86400000")
                .build();
    }

    @Bean
    public NewTopic paymentsRetryTopic() {
        return TopicBuilder.name(TOPIC_PAYMENTS_RETRY)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "86400000")
                .build();
    }

    @Bean
    public NewTopic usersRetryTopic() {
        return TopicBuilder.name(TOPIC_USERS_RETRY)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "86400000")
                .build();
    }

    @Bean
    public NewTopic receiptsRetryTopic() {
        return TopicBuilder.name(TOPIC_RECEIPTS_RETRY)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "86400000")
                .build();
    }
}
