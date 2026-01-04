package com.extractor.unraveldocs.ocrprocessing.events;

import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.brokers.kafka.producer.KafkaMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for OCR processing events to Kafka.
 * Publishes OCR requests to the documents topic for processing.
 * Uses collectionId as partition key to ensure documents in the same collection
 * are processed in order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class OcrEventPublisher {

    private final KafkaMessageProducer<OcrRequestedEvent> kafkaMessageProducer;

    /**
     * Publish an OCR request event to Kafka.
     *
     * @param event The OCR request event to publish
     * @return CompletableFuture that completes when the message is sent
     */
    public CompletableFuture<Void> publishOcrRequest(OcrRequestedEvent event) {
        String correlationId = UUID.randomUUID().toString();

        log.info("Publishing OCR request for document: {}, collection: {}, correlationId: {}",
                event.getDocumentId(), event.getCollectionId(), correlationId);

        // Use collectionId as partition key to ensure ordering within a collection
        Message<OcrRequestedEvent> message = Message.of(
                event,
                KafkaTopicConfig.TOPIC_DOCUMENTS,
                event.getCollectionId());

        return kafkaMessageProducer.send(message)
                .thenAccept(result -> {
                    if (result.success()) {
                        log.debug("OCR event published successfully for document: {}, partition: {}, offset: {}",
                                event.getDocumentId(), result.partition(), result.offset());
                    } else {
                        log.warn("OCR event publish may have failed for document: {}, error: {}",
                                event.getDocumentId(), result.errorMessage());
                    }
                })
                .exceptionally(e -> {
                    log.error("Failed to publish OCR event for document: {}. Error: {}",
                            event.getDocumentId(), e.getMessage(), e);
                    return null;
                });
    }

    /**
     * Publish a batch of OCR request events to Kafka.
     *
     * @param events The events to publish
     */
    public void publishBatch(Iterable<OcrRequestedEvent> events) {
        for (OcrRequestedEvent event : events) {
            publishOcrRequest(event);
        }
    }
}
