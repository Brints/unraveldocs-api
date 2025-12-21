package com.extractor.unraveldocs.messagequeuing.kafka.config;

import com.extractor.unraveldocs.messagequeuing.config.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jspecify.annotations.NonNull;
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
 * Kafka consumer configuration.
 * Only loaded when Kafka bootstrap servers are configured.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaConsumerConfig {
    
    private final MessagingProperties messagingProperties;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id:unraveldocs-group}")
    private String groupId;
    
    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;
    
    @Value("${spring.kafka.consumer.enable-auto-commit:false}")
    private boolean enableAutoCommit;
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        var consumerProps = messagingProperties.getKafka().getConsumer();

        // Connection settings
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // Offset handling
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        
        // Deserialization with error handling
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);
        
        // JSON deserializer settings
        configProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.extractor.unraveldocs.*");
        configProps.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // Performance tuning - use configurable values
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerProps.getMaxPollRecords());
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // Session and heartbeat settings
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumerProps.getSessionTimeoutMs());
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, consumerProps.getHeartbeatIntervalMs());

        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        
        // Manual acknowledgment for exactly-once semantics
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Concurrency (number of consumer threads) - configurable
        factory.setConcurrency(messagingProperties.getKafka().getConsumer().getConcurrency());

        log.info("Kafka listener container configured with concurrency: {}",
                messagingProperties.getKafka().getConsumer().getConcurrency());

        return factory;
    }
    
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        var retryProps = messagingProperties.getKafka().getRetry();

        // Configure exponential backoff
        ExponentialBackOff backOff = new ExponentialBackOff(
                retryProps.getInitialIntervalMs(),
                retryProps.getMultiplier()
        );
        backOff.setMaxInterval(retryProps.getMaxIntervalMs());
        backOff.setMaxElapsedTime(retryProps.getMaxIntervalMs() * retryProps.getMaxAttempts());

        // Dead Letter Publishing Recoverer - routes to DLQ after all retries exhausted
        DefaultErrorHandler errorHandler = getErrorHandler(kafkaTemplate, backOff);

        // Don't retry for certain exceptions (non-recoverable)
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                ClassCastException.class
        );

        log.info("Kafka error handler configured with exponential backoff: initialInterval={}ms, multiplier={}, maxInterval={}ms",
                retryProps.getInitialIntervalMs(),
                retryProps.getMultiplier(),
                retryProps.getMaxIntervalMs());

        return errorHandler;
    }

    private static @NonNull DefaultErrorHandler getErrorHandler(KafkaTemplate<String, Object> kafkaTemplate, ExponentialBackOff backOff) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    String dlqTopic = record.topic() + "-dlq";
                    log.error(
                            "Exhausted retries. Routing to DLQ: {}. Original topic: {}, Partition: {}, Offset: {}, Error: {}",
                            dlqTopic,
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            ex.getMessage()
                    );
                    return new org.apache.kafka.common.TopicPartition(dlqTopic, -1);
                }
        );

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
