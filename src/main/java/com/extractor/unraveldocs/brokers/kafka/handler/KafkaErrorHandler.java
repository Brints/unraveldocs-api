package com.extractor.unraveldocs.brokers.kafka.handler;

import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Kafka error handler with Dead Letter Queue (DLQ) support.
 * Routes failed messages to appropriate DLQ topics for later analysis or retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaErrorHandler {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String HEADER_ORIGINAL_TOPIC = "original-topic";
    private static final String HEADER_EXCEPTION_MESSAGE = "exception-message";
    private static final String HEADER_EXCEPTION_CLASS = "exception-class";
    private static final String HEADER_FAILURE_TIMESTAMP = "failure-timestamp";
    private static final String HEADER_RETRY_COUNT = "retry-count";
    
    /**
     * Route a failed message to its Dead Letter Queue.
     *
     * @param record The failed consumer record
     * @param exception The exception that caused the failure
     */
    public void routeToDlq(ConsumerRecord<?, ?> record, Exception exception) {
        String dlqTopic = getDlqTopic(record.topic());
        
        log.warn("Routing failed message to DLQ. Original topic: {}, DLQ topic: {}, Error: {}",
                record.topic(), dlqTopic, exception.getMessage());
        
        try {
            ProducerRecord<String, Object> dlqRecord = new ProducerRecord<>(
                    dlqTopic,
                    null,
                    record.timestamp(),
                    record.key() != null ? record.key().toString() : null,
                    record.value()
            );
            
            // Preserve original headers
            for (Header header : record.headers()) {
                dlqRecord.headers().add(header);
            }
            
            // Add DLQ-specific headers
            addDlqHeaders(dlqRecord, record, exception);
            
            kafkaTemplate.send(dlqRecord)
                    .thenAccept(result -> log.debug(
                            "Successfully routed message to DLQ: {}, offset: {}",
                            dlqTopic, result.getRecordMetadata().offset()))
                    .exceptionally(ex -> {
                        log.error("Failed to route message to DLQ: {}", ex.getMessage(), ex);
                        return null;
                    });
            
        } catch (Exception e) {
            log.error("Critical: Failed to create DLQ record: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retry a message from the DLQ to its original topic.
     *
     * @param record The DLQ record to retry
     * @return true if the message was successfully re-queued
     */
    public boolean retryFromDlq(ConsumerRecord<String, Object> record) {
        String originalTopic = extractOriginalTopic(record);
        
        if (originalTopic == null) {
            log.error("Cannot retry message: original-topic header missing");
            return false;
        }
        
        int currentRetryCount = extractRetryCount(record);
        
        log.info("Retrying message from DLQ. Original topic: {}, Retry count: {}",
                originalTopic, currentRetryCount + 1);
        
        try {
            ProducerRecord<String, Object> retryRecord = new ProducerRecord<>(
                    originalTopic,
                    record.key(),
                    record.value()
            );
            
            // Copy headers except DLQ-specific ones
            for (Header header : record.headers()) {
                String headerKey = header.key();
                if (!headerKey.startsWith("exception-") && !headerKey.equals(HEADER_FAILURE_TIMESTAMP)) {
                    retryRecord.headers().add(header);
                }
            }
            
            // Increment retry count
            retryRecord.headers().add(
                    HEADER_RETRY_COUNT,
                    String.valueOf(currentRetryCount + 1).getBytes(StandardCharsets.UTF_8)
            );
            
            kafkaTemplate.send(retryRecord).get();
            return true;
            
        } catch (Exception e) {
            log.error("Failed to retry message from DLQ: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the DLQ topic name for a given source topic.
     */
    private String getDlqTopic(String sourceTopic) {
        // Handle retry topics - route them to corresponding DLQ
        if (sourceTopic.endsWith("-retry")) {
            return sourceTopic.replace("-retry", "-dlq");
        }

        return switch (sourceTopic) {
            case KafkaTopicConfig.TOPIC_EMAILS -> KafkaTopicConfig.TOPIC_EMAILS_DLQ;
            case KafkaTopicConfig.TOPIC_DOCUMENTS -> KafkaTopicConfig.TOPIC_DOCUMENTS_DLQ;
            case KafkaTopicConfig.TOPIC_PAYMENTS -> KafkaTopicConfig.TOPIC_PAYMENTS_DLQ;
            case KafkaTopicConfig.TOPIC_USERS -> KafkaTopicConfig.TOPIC_USERS_DLQ;
            default -> sourceTopic + "-dlq";
        };
    }
    
    /**
     * Get the retry topic name for a given source topic.
     * Returns null if retry topics are not applicable for this topic.
     */
    public String getRetryTopic(String sourceTopic) {
        return switch (sourceTopic) {
            case KafkaTopicConfig.TOPIC_EMAILS -> KafkaTopicConfig.TOPIC_EMAILS_RETRY;
            case KafkaTopicConfig.TOPIC_DOCUMENTS -> KafkaTopicConfig.TOPIC_DOCUMENTS_RETRY;
            case KafkaTopicConfig.TOPIC_PAYMENTS -> KafkaTopicConfig.TOPIC_PAYMENTS_RETRY;
            case KafkaTopicConfig.TOPIC_USERS -> KafkaTopicConfig.TOPIC_USERS_RETRY;
            default -> null;
        };
    }

    /**
     * Route a failed message to the retry topic for delayed retry.
     * If max retries exceeded or no retry topic available, routes to DLQ.
     *
     * @param record The failed consumer record
     * @param exception The exception that caused the failure
     * @param maxRetries Maximum number of retries before routing to DLQ
     */
    public void routeToRetryOrDlq(ConsumerRecord<?, ?> record, Exception exception, int maxRetries) {
        int currentRetryCount = extractRetryCount(record);

        String retryTopic = getRetryTopic(record.topic());

        // If we've exhausted retries or no retry topic exists, route to DLQ
        if (currentRetryCount >= maxRetries || retryTopic == null) {
            routeToDlq(record, exception);
            return;
        }

        log.info("Routing failed message to retry topic. Original topic: {}, Retry topic: {}, Attempt: {}/{}",
                record.topic(), retryTopic, currentRetryCount + 1, maxRetries);

        try {
            ProducerRecord<String, Object> retryRecord = new ProducerRecord<>(
                    retryTopic,
                    null,
                    record.timestamp(),
                    record.key() != null ? record.key().toString() : null,
                    record.value()
            );

            // Preserve original headers
            for (Header header : record.headers()) {
                retryRecord.headers().add(header);
            }

            // Add retry metadata
            retryRecord.headers().add(
                    HEADER_ORIGINAL_TOPIC,
                    record.topic().getBytes(StandardCharsets.UTF_8)
            );
            retryRecord.headers().add(
                    HEADER_RETRY_COUNT,
                    String.valueOf(currentRetryCount + 1).getBytes(StandardCharsets.UTF_8)
            );
            retryRecord.headers().add(
                    HEADER_EXCEPTION_MESSAGE,
                    (exception.getMessage() != null ? exception.getMessage() : "Unknown error")
                            .getBytes(StandardCharsets.UTF_8)
            );

            kafkaTemplate.send(retryRecord)
                    .thenAccept(result -> log.debug(
                            "Successfully routed message to retry topic: {}, offset: {}",
                            retryTopic, result.getRecordMetadata().offset()))
                    .exceptionally(ex -> {
                        log.error("Failed to route message to retry topic, routing to DLQ: {}", ex.getMessage());
                        routeToDlq(record, exception);
                        return null;
                    });

        } catch (Exception e) {
            log.error("Critical: Failed to create retry record, routing to DLQ: {}", e.getMessage(), e);
            routeToDlq(record, exception);
        }
    }

    /**
     * Extract the retry count from a consumer record.
     */
    private int extractRetryCount(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(HEADER_RETRY_COUNT);
        if (header != null) {
            try {
                return Integer.parseInt(new String(header.value(), StandardCharsets.UTF_8));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Add Dead Letter Queue headers to a record.
     */
    private void addDlqHeaders(ProducerRecord<String, Object> dlqRecord,
                                ConsumerRecord<?, ?> originalRecord,
                                Exception exception) {
        dlqRecord.headers().add(
                HEADER_ORIGINAL_TOPIC,
                originalRecord.topic().getBytes(StandardCharsets.UTF_8)
        );
        
        dlqRecord.headers().add(
                HEADER_EXCEPTION_MESSAGE,
                (exception.getMessage() != null ? exception.getMessage() : "Unknown error")
                        .getBytes(StandardCharsets.UTF_8)
        );
        
        dlqRecord.headers().add(
                HEADER_EXCEPTION_CLASS,
                exception.getClass().getName().getBytes(StandardCharsets.UTF_8)
        );
        
        dlqRecord.headers().add(
                HEADER_FAILURE_TIMESTAMP,
                Instant.now().toString().getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * Extract the original topic from a DLQ record.
     */
    private String extractOriginalTopic(ConsumerRecord<String, Object> record) {
        Header header = record.headers().lastHeader(HEADER_ORIGINAL_TOPIC);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}
