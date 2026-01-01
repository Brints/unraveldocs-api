package com.extractor.unraveldocs.payment.receipt.events;

import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.service.ReceiptGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for receipt generation events.
 * Listens to receipt requests and triggers the receipt generation process.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class ReceiptMessageListener {

    private final ReceiptGenerationService receiptGenerationService;
    private final ReceiptEventMapper receiptEventMapper;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_RECEIPTS, groupId = "unraveldocs-receipt-group", containerFactory = "kafkaListenerContainerFactory")
    @SuppressWarnings("unchecked")
    public void receiveReceiptRequestedEvent(
            ConsumerRecord<String, BaseEvent<Object>> record,
            Acknowledgment acknowledgment) {
        BaseEvent<Object> event = record.value();

        // Handle generic type erasure - payload may be a LinkedHashMap
        ReceiptRequestedEvent payload;
        if (event.getPayload() instanceof Map) {
            payload = objectMapper.convertValue(event.getPayload(), ReceiptRequestedEvent.class);
        } else {
            payload = (ReceiptRequestedEvent) event.getPayload();
        }

        String correlationId = event.getMetadata() != null ? event.getMetadata().getCorrelationId() : record.key();

        log.info("Received receipt request via Kafka for payment: {}, provider: {}, correlationId: {}",
                payload.getExternalPaymentId(),
                payload.getPaymentProvider(),
                correlationId);

        try {
            // Convert event to ReceiptData for processing
            ReceiptData receiptData = receiptEventMapper.toReceiptData(payload);

            // Process the receipt generation
            Receipt receipt = receiptGenerationService.processReceiptGeneration(receiptData);

            // Acknowledge the message after successful processing
            acknowledgment.acknowledge();

            if (receipt != null) {
                log.info("Receipt generation completed successfully for payment: {}, receipt: {}, correlationId: {}",
                        payload.getExternalPaymentId(),
                        receipt.getReceiptNumber(),
                        correlationId);
            } else {
                log.info("Receipt already exists for payment: {}, correlationId: {}",
                        payload.getExternalPaymentId(),
                        correlationId);
            }

        } catch (Exception e) {
            log.error("Error processing receipt request for payment: {}, provider: {}, correlationId: {}, error: {}",
                    payload.getExternalPaymentId(),
                    payload.getPaymentProvider(),
                    correlationId,
                    e.getMessage(), e);
            // Do not acknowledge - message will be redelivered or sent to DLQ
            throw e;
        }
    }
}
