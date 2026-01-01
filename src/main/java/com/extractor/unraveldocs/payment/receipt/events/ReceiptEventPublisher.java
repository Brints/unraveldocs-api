package com.extractor.unraveldocs.payment.receipt.events;

import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventMetadata;
import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.brokers.kafka.producer.KafkaMessageProducer;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for publishing receipt generation events to Kafka.
 * This replaces the @Async approach with an event-driven architecture.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class ReceiptEventPublisher {

    private final KafkaMessageProducer<BaseEvent<ReceiptRequestedEvent>> kafkaMessageProducer;
    private final ReceiptEventMapper receiptEventMapper;
    private final SanitizeLogging sanitizer;

    /**
     * Publish a receipt generation request to Kafka.
     * The receipt will be generated asynchronously by a Kafka consumer.
     *
     * @param receiptData The receipt data from the payment
     */
    public void publishReceiptRequest(ReceiptData receiptData) {
        String correlationId = UUID.randomUUID().toString();

        log.info("Publishing receipt request for payment: {}, provider: {}, correlationId: {}",
                sanitizer.sanitizeLogging(receiptData.getExternalPaymentId()),
                sanitizer.sanitizeLoggingObject(receiptData.getPaymentProvider()),
                sanitizer.sanitizeLogging(correlationId));

        try {
            ReceiptRequestedEvent payload = receiptEventMapper.toReceiptRequestedEvent(receiptData);

            EventMetadata metadata = EventMetadata.builder()
                    .eventType("ReceiptRequested")
                    .eventSource("ReceiptEventPublisher")
                    .eventTimestamp(System.currentTimeMillis())
                    .correlationId(correlationId)
                    .build();

            BaseEvent<ReceiptRequestedEvent> event = BaseEvent.<ReceiptRequestedEvent>builder()
                    .metadata(metadata)
                    .payload(payload)
                    .build();

            // Use the external payment ID as the partition key for ordering guarantees
            Message<BaseEvent<ReceiptRequestedEvent>> message = Message.of(
                    event,
                    KafkaTopicConfig.TOPIC_RECEIPTS,
                    receiptData.getExternalPaymentId()
            );

            kafkaMessageProducer.send(message)
                    .thenAccept(result -> log.info(
                            "Receipt request published successfully for payment: {}, correlationId: {}",
                            sanitizer.sanitizeLogging(receiptData.getExternalPaymentId()),
                            sanitizer.sanitizeLogging(correlationId)))
                    .exceptionally(ex -> {
                        log.error("Failed to publish receipt request for payment: {}, correlationId: {}, error: {}",
                                sanitizer.sanitizeLogging(receiptData.getExternalPaymentId()),
                                sanitizer.sanitizeLogging(correlationId), ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.error("Error publishing receipt request for payment: {}, error: {}",
                    sanitizer.sanitizeLogging(receiptData.getExternalPaymentId()), e.getMessage(), e);
            throw new RuntimeException("Failed to publish receipt request", e);
        }
    }
}

