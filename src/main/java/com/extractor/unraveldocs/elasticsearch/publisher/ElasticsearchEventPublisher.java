package com.extractor.unraveldocs.elasticsearch.publisher;

import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.core.MessageBrokerFactory;
import com.extractor.unraveldocs.brokers.core.MessageBrokerType;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.elasticsearch.events.ElasticsearchIndexEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * Service for publishing Elasticsearch indexing events to Kafka.
 * Provides methods to publish index events for different entity types.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class ElasticsearchEventPublisher {

    private final MessageBrokerFactory messageBrokerFactory;
    private final JsonMapper jsonMapper;

    /**
     * Publishes a document index event.
     *
     * @param event The indexing event to publish
     */
    public void publishDocumentIndexEvent(ElasticsearchIndexEvent event) {
        publishEvent(event, "elasticsearch.index.document");
    }

    /**
     * Publishes a user index event.
     *
     * @param event The indexing event to publish
     */
    public void publishUserIndexEvent(ElasticsearchIndexEvent event) {
        publishEvent(event, "elasticsearch.index.user");
    }

    /**
     * Publishes a payment index event.
     *
     * @param event The indexing event to publish
     */
    public void publishPaymentIndexEvent(ElasticsearchIndexEvent event) {
        publishEvent(event, "elasticsearch.index.payment");
    }

    /**
     * Publishes a subscription index event.
     *
     * @param event The indexing event to publish
     */
    public void publishSubscriptionIndexEvent(ElasticsearchIndexEvent event) {
        publishEvent(event, "elasticsearch.index.subscription");
    }

    /**
     * Publishes an indexing event based on the index type.
     *
     * @param event The indexing event to publish
     */
    public void publishEvent(ElasticsearchIndexEvent event) {
        switch (event.getIndexType()) {
            case DOCUMENT -> publishDocumentIndexEvent(event);
            case USER -> publishUserIndexEvent(event);
            case PAYMENT -> publishPaymentIndexEvent(event);
            case SUBSCRIPTION -> publishSubscriptionIndexEvent(event);
        }
    }

    private void publishEvent(ElasticsearchIndexEvent event, String eventType) {
        log.debug("Publishing Elasticsearch {} event for document ID: {}, action: {}",
                event.getIndexType(), event.getDocumentId(), event.getAction());

        Message<ElasticsearchIndexEvent> message = Message.of(
                event,
                KafkaTopicConfig.TOPIC_ELASTICSEARCH,
                event.getDocumentId(), // Use document ID as key for ordering
                Map.of("event-type", eventType));

        messageBrokerFactory.<ElasticsearchIndexEvent>getProducer(MessageBrokerType.KAFKA)
                .send(message)
                .thenAccept(result -> {
                    if (result.success()) {
                        log.info("Published Elasticsearch {} event for document ID: {}",
                                event.getIndexType(), event.getDocumentId());
                    } else {
                        log.error("Failed to publish Elasticsearch event: {}", result.errorMessage());
                    }
                });
    }

    /**
     * Converts an object to JSON string for event payload.
     *
     * @param object The object to convert
     * @return JSON string representation
     */
    public String toJsonPayload(Object object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Failed to convert object to JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize object for Elasticsearch indexing", e);
        }
    }
}
