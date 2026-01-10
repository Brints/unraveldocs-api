package com.extractor.unraveldocs.brokers.kafka.events;

import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.core.MessageBrokerFactory;
import com.extractor.unraveldocs.brokers.core.MessageBrokerType;
import com.extractor.unraveldocs.brokers.core.MessagingException;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for publishing events to Kafka.
 * Provides a unified way to publish different types of events to their
 * respective topics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class EventPublisherService {

    private final MessageBrokerFactory messageBrokerFactory;

    /**
     * Publishes an event to Kafka.
     *
     * @param topic The Kafka topic to publish to
     * @param event The event to publish
     * @param <T>   The payload type
     * @throws MessagingException if publishing fails
     */
    public <T> void publishEvent(String topic, BaseEvent<T> event) {
        try {
            Object payload = event.getPayload();
            String eventType = event.getMetadata().getEventType();
            String correlationId = event.getMetadata().getCorrelationId();

            Message<Object> message = Message.of(
                    payload,
                    topic,
                    correlationId,
                    Map.of("event-type", eventType));

            messageBrokerFactory.getProducer(MessageBrokerType.KAFKA)
                    .send(message)
                    .thenAccept(result -> {
                        if (result.success()) {
                            log.info("Published event of type '{}' with correlationId '{}' to topic '{}'",
                                    eventType, correlationId, topic);
                        } else {
                            log.error("Failed to publish event to topic '{}': {}",
                                    topic, result.errorMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to publish event. CorrelationId: '{}', Topic: '{}'. Error: {}",
                    event.getMetadata().getCorrelationId(), topic, e.getMessage(), e);
            throw MessagingException.sendFailed(
                    MessageBrokerType.KAFKA,
                    event.getMetadata().getCorrelationId(),
                    topic,
                    e);
        }
    }

    /**
     * Publishes a user event to the users topic.
     */
    public <T> void publishUserEvent(BaseEvent<T> event) {
        publishEvent(KafkaTopicConfig.TOPIC_USERS, event);
    }

    /**
     * Publishes a team event to the team events topic.
     */
    public <T> void publishTeamEvent(BaseEvent<T> event) {
        publishEvent(KafkaTopicConfig.TOPIC_TEAM_EVENTS, event);
    }

    /**
     * Publishes an admin event to the admin events topic.
     */
    public <T> void publishAdminEvent(BaseEvent<T> event) {
        publishEvent(KafkaTopicConfig.TOPIC_ADMIN_EVENTS, event);
    }

    /**
     * Publishes an OCR event to the OCR topic.
     */
    public <T> void publishOcrEvent(BaseEvent<T> event) {
        publishEvent(KafkaTopicConfig.TOPIC_OCR, event);
    }
}
