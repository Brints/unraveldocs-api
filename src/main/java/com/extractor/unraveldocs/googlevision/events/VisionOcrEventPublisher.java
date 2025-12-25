package com.extractor.unraveldocs.googlevision.events;

import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.kafka.producer.KafkaMessageProducer;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for Vision OCR events to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class VisionOcrEventPublisher {

    private final KafkaMessageProducer<VisionOcrRequestedEvent> kafkaMessageProducer;

    /**
     * Publish a Vision OCR request event.
     *
     * @param event The event to publish
     * @return CompletableFuture that completes when the message is sent
     */
    public CompletableFuture<Void> publishOcrRequest(VisionOcrRequestedEvent event) {
        if (event.getCorrelationId() == null) {
            event.setCorrelationId(UUID.randomUUID().toString());
        }

        log.info("Publishing Vision OCR request for document: {}, correlationId: {}",
                event.getDocumentId(), event.getCorrelationId());

        Message<VisionOcrRequestedEvent> message = Message.of(
                event,
                KafkaTopicConfig.TOPIC_DOCUMENTS,
                event.getDocumentId());

        return kafkaMessageProducer.send(message)
                .thenAccept(result -> {
                    if (result.success()) {
                        log.debug("Vision OCR event published successfully for document: {}",
                                event.getDocumentId());
                    } else {
                        log.warn("Vision OCR event publish may have failed for document: {}",
                                event.getDocumentId());
                    }
                })
                .exceptionally(e -> {
                    log.error("Failed to publish Vision OCR event for document: {}. Error: {}",
                            event.getDocumentId(), e.getMessage(), e);
                    return null;
                });
    }

    /**
     * Publish a batch of Vision OCR request events.
     *
     * @param events The events to publish
     */
    public void publishBatch(Iterable<VisionOcrRequestedEvent> events) {
        for (VisionOcrRequestedEvent event : events) {
            publishOcrRequest(event);
        }
    }
}
