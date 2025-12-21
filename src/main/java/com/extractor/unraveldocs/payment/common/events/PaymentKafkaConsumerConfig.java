package com.extractor.unraveldocs.payment.common.events;

import com.extractor.unraveldocs.messagequeuing.config.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration optimized for high-throughput payment processing.
 *
 * <h2>Throughput Optimizations:</h2>
 * <ul>
 *   <li><b>Batch Processing:</b> Consumes up to 500 records per poll</li>
 *   <li><b>Concurrency:</b> 6 concurrent consumers per listener</li>
 *   <li><b>Manual Acknowledgment:</b> Batch acknowledgment for efficiency</li>
 *   <li><b>Retry with Backoff:</b> Exponential backoff before DLQ</li>
 * </ul>
 *
 * <h2>Scaling for Higher TPS:</h2>
 * <ul>
 *   <li>1,000 TPS: 6 consumers, 12 partitions</li>
 *   <li>5,000 TPS: 12 consumers, 24 partitions</li>
 *   <li>10,000+ TPS: 24+ consumers, 48+ partitions, multiple instances</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class PaymentKafkaConsumerConfig {

    private final MessagingProperties messagingProperties;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ==================== Consumer Configuration ====================

    /**
     * Max records to fetch per poll. Higher = better throughput, more memory.
     * Recommended: 100-500 for payment processing.
     */
    @Value("${payment.kafka.consumer.max-poll-records:500}")
    private int maxPollRecords;

    /**
     * Max time to wait for records. Lower = lower latency, higher = better batching.
     */
    @Value("${payment.kafka.consumer.fetch-max-wait-ms:500}")
    private int fetchMaxWaitMs;

    /**
     * Session timeout for consumer group coordination.
     */
    @Value("${payment.kafka.consumer.session-timeout-ms:30000}")
    private int sessionTimeoutMs;

    /**
     * Heartbeat interval (should be 1/3 of session timeout).
     */
    @Value("${payment.kafka.consumer.heartbeat-interval-ms:10000}")
    private int heartbeatIntervalMs;

    /**
     * Max time between polls before consumer is considered dead.
     */
    @Value("${payment.kafka.consumer.max-poll-interval-ms:300000}")
    private int maxPollIntervalMs;

    // ==================== Consumer Factory ====================

    @Bean
    public ConsumerFactory<String, PaymentEvent> paymentEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Connection
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Consumer group
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "unraveldocs-payment-processor");

        // Offset handling
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Deserialization
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);

        // JSON deserializer config
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.extractor.unraveldocs.payment.*");
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // ==================== Throughput Optimizations ====================

        // Fetch more records per poll for better throughput
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

        // Fetch settings
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800); // 50MB
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 10485760); // 10MB per partition

        // Session and heartbeat
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // ==================== Batch Listener Container Factory ====================

    /**
     * Batch listener factory for high-throughput payment event processing.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent>
            paymentBatchListenerContainerFactory(
                    ConsumerFactory<String, PaymentEvent> paymentEventConsumerFactory,
                    KafkaTemplate<String, Object> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(paymentEventConsumerFactory);

        // Enable batch listening for throughput
        factory.setBatchListener(true);

        // Manual acknowledgment for reliability
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Concurrency - number of consumer threads per listener
        // Can be overridden per-listener with @KafkaListener(concurrency = "...")
        factory.setConcurrency(6);

        // Error handling with DLQ
        factory.setCommonErrorHandler(createErrorHandler(kafkaTemplate));

        log.info("Created payment batch listener factory with concurrency=6, batchListener=true");

        return factory;
    }

    // ==================== Error Handler with DLQ ====================

    private DefaultErrorHandler createErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Dead Letter Queue recoverer
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    // Route to appropriate DLQ based on original topic
                    String originalTopic = record.topic();
                    if (originalTopic.contains("webhook")) {
                        return new org.apache.kafka.common.TopicPartition(
                                PaymentTopicConfig.TOPIC_PAYMENT_WEBHOOKS_DLQ,
                                record.partition() % 3
                        );
                    }
                    return new org.apache.kafka.common.TopicPartition(
                            PaymentTopicConfig.TOPIC_PAYMENT_EVENTS_DLQ,
                            record.partition() % 3
                    );
                }
        );

        // Exponential backoff: 1s, 2s, 4s, 8s, 16s (max 5 retries)
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(30000L); // Max 30 seconds total

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Don't retry these exceptions
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );

        log.info("Created error handler with exponential backoff and DLQ");

        return errorHandler;
    }
}

