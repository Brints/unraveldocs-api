package com.extractor.unraveldocs.brokers.kafka.producer;

import com.extractor.unraveldocs.brokers.config.MessagingProperties;
import com.extractor.unraveldocs.brokers.core.*;
import com.extractor.unraveldocs.brokers.kafka.metrics.KafkaMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka implementation of the MessageProducer interface.
 * Provides async and sync message sending capabilities via KafkaTemplate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaMessageProducer<T> implements MessageProducer<T> {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MessagingProperties messagingProperties;
    private final KafkaMetrics kafkaMetrics;

    @Override
    public MessageBrokerType getBrokerType() {
        return MessageBrokerType.KAFKA;
    }
    
    @Override
    public CompletableFuture<MessageResult> send(Message<T> message) {
        log.debug("Sending message to topic: {}, key: {}, id: {}",
                message.topic(), message.key(), message.id());
        
        ProducerRecord<String, Object> record = createProducerRecord(message);
        Timer.Sample timer = kafkaMetrics.startSendTimer();

        return kafkaTemplate.send(record)
                .thenApply(result -> {
                    kafkaMetrics.stopSendTimer(timer, message.topic());
                    kafkaMetrics.recordMessageSent(message.topic());
                    return handleSuccess(result);
                })
                .exceptionally(ex -> {
                    kafkaMetrics.recordMessageFailed(message.topic());
                    return handleFailure(message, ex);
                });
    }
    
    @Override
    public MessageResult sendAndWait(Message<T> message) {
        try {
            long timeoutSeconds = messagingProperties.getKafka().getProducer().getSendTimeoutSeconds();
            Timer.Sample timer = kafkaMetrics.startSendTimer();

            log.debug("Sending message synchronously to topic: {}, key: {}, id: {}, timeout: {}s",
                    message.topic(), message.key(), message.id(), timeoutSeconds);

            ProducerRecord<String, Object> record = createProducerRecord(message);
            
            SendResult<String, Object> result = kafkaTemplate.send(record)
                    .get(timeoutSeconds, TimeUnit.SECONDS);

            kafkaMetrics.stopSendTimer(timer, message.topic());
            kafkaMetrics.recordMessageSent(message.topic());

            return handleSuccess(result);
            
        } catch (Exception e) {
            kafkaMetrics.recordMessageFailed(message.topic());
            log.error("Failed to send message synchronously: {}", e.getMessage(), e);
            throw MessagingException.sendFailed(
                    MessageBrokerType.KAFKA,
                    message.id(),
                    message.topic(),
                    e
            );
        }
    }
    
    /**
     * Create a Kafka ProducerRecord from our Message wrapper.
     */
    private ProducerRecord<String, Object> createProducerRecord(Message<T> message) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                message.topic(),
                null, // partition (let Kafka decide based on key)
                message.timestamp().toEpochMilli(),
                message.key(),
                message.payload()
        );
        
        // Add message ID header
        record.headers().add(new RecordHeader(
                "message-id",
                message.id().getBytes(StandardCharsets.UTF_8)
        ));
        
        // Add custom headers
        if (message.headers() != null) {
            message.headers().forEach((key, value) -> {
                if (value != null) {
                    record.headers().add(new RecordHeader(
                            key,
                            value.getBytes(StandardCharsets.UTF_8)
                    ));
                }
            });
        }
        
        return record;
    }
    
    /**
     * Handle successful send result.
     */
    private MessageResult handleSuccess(SendResult<String, Object> result) {
        var metadata = result.getRecordMetadata();
        
        String messageId = extractMessageId(result);
        
        log.debug("Message sent successfully. Topic: {}, Partition: {}, Offset: {}",
                metadata.topic(), metadata.partition(), metadata.offset());
        
        return MessageResult.success(
                messageId,
                metadata.topic(),
                metadata.partition(),
                metadata.offset()
        );
    }
    
    /**
     * Handle send failure.
     */
    private MessageResult handleFailure(Message<T> message, Throwable ex) {
        log.error("Failed to send message to topic {}: {}", message.topic(), ex.getMessage());
        return MessageResult.failure(message.id(), message.topic(), ex);
    }
    
    /**
     * Extract message ID from headers.
     */
    private String extractMessageId(SendResult<String, Object> result) {
        var header = result.getProducerRecord().headers().lastHeader("message-id");
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}
