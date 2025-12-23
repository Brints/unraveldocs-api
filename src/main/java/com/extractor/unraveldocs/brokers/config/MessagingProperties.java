package com.extractor.unraveldocs.brokers.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the messaging system.
 * Provides centralized configuration for all message brokers.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "messaging")
public class MessagingProperties {
    
    /**
     * Default broker to use when not specified.
     * Options: KAFKA, RABBITMQ
     */
    private String defaultBroker = "KAFKA";
    
    /**
     * Whether to enable messaging features.
     */
    private boolean enabled = true;
    
    /**
     * Kafka-specific configuration.
     */
    private KafkaProperties kafka = new KafkaProperties();
    
    /**
     * RabbitMQ-specific configuration
     */
    private RabbitMQProperties rabbitmq = new RabbitMQProperties();
    
    @Getter
    @Setter
    public static class KafkaProperties {
        /**
         * Whether Kafka messaging is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Topic prefix for all topics.
         */
        private String topicPrefix = "unraveldocs";
        
        /**
         * Default number of partitions for new topics.
         */
        private int defaultPartitions = 3;
        
        /**
         * Default replication factor for new topics.
         */
        private short defaultReplicationFactor = 1;
        
        /**
         * Default retention period in milliseconds.
         */
        private long defaultRetentionMs = 604800000L; // 7 days

        /**
         * Consumer configuration.
         */
        private ConsumerProperties consumer = new ConsumerProperties();

        /**
         * Producer configuration.
         */
        private ProducerProperties producer = new ProducerProperties();

        /**
         * Retry configuration.
         */
        private RetryProperties retry = new RetryProperties();
    }

    @Getter
    @Setter
    public static class ConsumerProperties {
        /**
         * Number of concurrent consumer threads.
         */
        private int concurrency = 3;

        /**
         * Maximum number of records to poll at once.
         */
        private int maxPollRecords = 500;

        /**
         * Session timeout in milliseconds.
         */
        private int sessionTimeoutMs = 30000;

        /**
         * Heartbeat interval in milliseconds.
         */
        private int heartbeatIntervalMs = 10000;
    }

    @Getter
    @Setter
    public static class ProducerProperties {
        /**
         * Timeout for synchronous send operations in seconds.
         */
        private long sendTimeoutSeconds = 30;

        /**
         * Whether to enable transactional producers.
         */
        private boolean transactionalEnabled = false;

        /**
         * Transactional ID prefix (only used if transactionalEnabled is true).
         */
        private String transactionalIdPrefix = "unraveldocs-tx-";
    }

    @Getter
    @Setter
    public static class RetryProperties {
        /**
         * Maximum number of retry attempts before sending to DLQ.
         */
        private int maxAttempts = 3;

        /**
         * Initial backoff interval in milliseconds.
         */
        private long initialIntervalMs = 1000;

        /**
         * Backoff multiplier for exponential backoff.
         */
        private double multiplier = 2.0;

        /**
         * Maximum backoff interval in milliseconds.
         */
        private long maxIntervalMs = 30000;

        /**
         * Whether to enable retry topics (intermediate retry before DLQ).
         */
        private boolean retryTopicsEnabled = true;
    }
    
    @Getter
    @Setter
    public static class RabbitMQProperties {
        /**
         * Whether RabbitMQ messaging is enabled.
         */
        private boolean enabled = false;
        
        /**
         * Exchange prefix for all exchanges.
         */
        private String exchangePrefix = "unraveldocs";
        
        /**
         * Queue prefix for all queues.
         */
        private String queuePrefix = "unraveldocs";
    }
}
