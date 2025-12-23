package com.extractor.unraveldocs.ocrprocessing.events;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.ocrprocessing.interfaces.ProcessOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for OCR processing events.
 * Uses Kafka for document/OCR processing due to:
 * - High throughput for batch document processing
 * - Better partitioning for parallel processing
 * - Stream processing capabilities
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrMessageListener {
    private final ProcessOcrService ocrService;
    private final SanitizeLogging s;

    @KafkaListener(
            topics = KafkaTopicConfig.TOPIC_DOCUMENTS,
            groupId = "unraveldocs-ocr-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void receiveOcrRequestedEvent(
            ConsumerRecord<String, OcrRequestedEvent> record,
            Acknowledgment acknowledgment
    ) {
        OcrRequestedEvent payload = record.value();
        String correlationId = record.key();
        
        log.info("Received OCR request via Kafka for collection ID: {}, document ID: {}. CorrelationId: {}",
                s.sanitizeLogging(payload.getCollectionId()),
                s.sanitizeLogging(payload.getDocumentId()),
                s.sanitizeLogging(correlationId));
        try {
            ocrService.processOcrRequest(payload.getCollectionId(), payload.getDocumentId());
            acknowledgment.acknowledge(); // Manual acknowledgment after successful processing
            log.debug("OCR processing completed successfully for document ID: {}", 
                    s.sanitizeLogging(payload.getDocumentId()));
        } catch (Exception e) {
            log.error("Error processing OCR request for collection ID: {}, document ID: {}. CorrelationId: {}. Error: {}",
                    s.sanitizeLogging(payload.getCollectionId()),
                    s.sanitizeLogging(payload.getDocumentId()),
                    s.sanitizeLogging(correlationId),
                    e.getMessage(), e);
            // Do not acknowledge - message will be redelivered or sent to DLQ
            throw e;
        }
    }
}

