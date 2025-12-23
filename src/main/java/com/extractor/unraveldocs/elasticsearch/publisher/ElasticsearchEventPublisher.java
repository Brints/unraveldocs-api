package com.extractor.unraveldocs.elasticsearch.publisher;

import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventTypes;
import com.extractor.unraveldocs.elasticsearch.events.ElasticsearchIndexEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Service for publishing Elasticsearch indexing events to RabbitMQ.
 * Provides methods to publish index events for different entity types.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class ElasticsearchEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    /**
     * Publishes a document index event.
     *
     * @param event The indexing event to publish
     */
    public void publishDocumentIndexEvent(ElasticsearchIndexEvent event) {
        publishEvent(event, EventTypes.ES_DOCUMENT_INDEX, "elasticsearch.index.document");
    }

    /**
     * Publishes a user index event.
     *
     * @param event The indexing event to publish
     */
    public void publishUserIndexEvent(ElasticsearchIndexEvent event) {
        publishEvent(event, EventTypes.ES_USER_INDEX, "elasticsearch.index.user");
    }

    /**
     * Publishes a payment index event.
     *
     * @param event The indexing event to publish
     */
    public void publishPaymentIndexEvent(ElasticsearchIndexEvent event) {
        publishEvent(event, EventTypes.ES_PAYMENT_INDEX, "elasticsearch.index.payment");
    }

    /**
     * Publishes a subscription index event.
     *
     * @param event The indexing event to publish
     */
    public void publishSubscriptionIndexEvent(ElasticsearchIndexEvent event) {
        publishEvent(event, EventTypes.ES_SUBSCRIPTION_INDEX, "elasticsearch.index.subscription");
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

    private void publishEvent(ElasticsearchIndexEvent event, String eventType, String routingKey) {
        log.debug("Publishing Elasticsearch {} event for document ID: {}, action: {}",
                event.getIndexType(), event.getDocumentId(), event.getAction());

        rabbitTemplate.convertAndSend(
                RabbitMQQueueConfig.ES_EVENTS_EXCHANGE,
                routingKey,
                event,
                message -> {
                    message.getMessageProperties().setType(eventType);
                    return message;
                });

        log.info("Published Elasticsearch {} event for document ID: {}",
                event.getIndexType(), event.getDocumentId());
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
