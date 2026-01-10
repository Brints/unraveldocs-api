package com.extractor.unraveldocs.user.eventlistener;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.auth.events.WelcomeEvent;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.brokers.kafka.events.EventHandler;
import com.extractor.unraveldocs.user.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for user events.
 * Handles user registration, password changes, deletion, and welcome events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class UserEventListener {

    private final Map<String, EventHandler<?>> eventHandlers;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void initializeHandlers() {
        log.info("Initialized event handlers for types: {}", eventHandlers.keySet());
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_USERS, groupId = "user-events-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserEvent(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        String eventType = extractEventType(record);
        Object payload = record.value();

        log.info("Received user event of type: {} with payload class: {}",
                eventType, payload.getClass().getSimpleName());

        try {
            processEvent(payload, eventType);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user event of type {}: {}", eventType, e.getMessage(), e);
            throw e; // Rethrow to trigger retry/DLQ handling
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void processEvent(T payload, String eventType) {
        String key = normalizeEventType(eventType, payload);

        // Try to convert payload to the correct type if it's a Map (from JSON
        // deserialization)
        Object typedPayload = convertPayloadIfNeeded(payload, eventType);

        EventHandler<Object> handler = (EventHandler<Object>) eventHandlers.get(key);
        if (handler != null) {
            handler.handleEvent(typedPayload);
            log.debug("Successfully processed event of type: {}", key);
        } else {
            log.warn("No handler found for event type: {}. Available: {}", key, eventHandlers.keySet());
        }
    }

    private Object convertPayloadIfNeeded(Object payload, String eventType) {
        if (payload instanceof Map) {
            try {
                Class<?> targetClass = getEventClass(eventType);
                if (targetClass != null) {
                    return objectMapper.convertValue(payload, targetClass);
                }
            } catch (Exception e) {
                log.warn("Failed to convert payload to specific type: {}", e.getMessage());
            }
        }
        return payload;
    }

    private Class<?> getEventClass(String eventType) {
        if (eventType == null)
            return null;
        String simpleName = eventType.contains(".") ? eventType.substring(eventType.lastIndexOf('.') + 1) : eventType;

        return switch (simpleName) {
            case "UserRegisteredEvent" -> UserRegisteredEvent.class;
            case "UserDeletionScheduledEvent" -> UserDeletionScheduledEvent.class;
            case "UserDeletedEvent" -> UserDeletedEvent.class;
            case "PasswordChangedEvent" -> PasswordChangedEvent.class;
            case "PasswordResetEvent" -> PasswordResetEvent.class;
            case "PasswordResetSuccessfulEvent" -> PasswordResetSuccessfulEvent.class;
            case "WelcomeEvent" -> WelcomeEvent.class;
            default -> null;
        };
    }

    private String extractEventType(ConsumerRecord<String, Object> record) {
        var header = record.headers().lastHeader("event-type");
        if (header != null) {
            return new String(header.value());
        }
        // Fallback to key if no header
        return record.key();
    }

    private String normalizeEventType(String eventType, Object payload) {
        if (eventType == null || eventType.isBlank()) {
            return payload.getClass().getSimpleName();
        }
        int dot = eventType.lastIndexOf('.');
        return dot >= 0 ? eventType.substring(dot + 1) : eventType;
    }
}
