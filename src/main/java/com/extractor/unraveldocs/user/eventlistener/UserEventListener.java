package com.extractor.unraveldocs.user.eventlistener;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.auth.events.WelcomeEvent;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventHandler;
import com.extractor.unraveldocs.user.events.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "#{T(com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig).USER_EVENTS_QUEUE}")
public class UserEventListener {

    private final Map<String, EventHandler<?>> eventHandlers;

    @PostConstruct
    public void initializeHandlers() {
        log.info("Initialized event handlers for types: {}", eventHandlers.keySet());
    }

    @RabbitHandler
    public void handleUserRegisteredEvent(UserRegisteredEvent payload, @Header("__TypeId__") String eventType) {
        processEvent(payload, eventType);
    }

    @RabbitHandler
    public void handleUserDeletionScheduledEvent(UserDeletionScheduledEvent payload, @Header("__TypeId__") String eventType) {
        processEvent(payload, eventType);
    }

    @RabbitHandler
    public void handleUserDeletedEvent(UserDeletedEvent payload, @Header("__TypeId__") String eventType) {
        processEvent(payload, eventType);
    }

    @RabbitHandler
    public void handlePasswordChangedEvent(PasswordChangedEvent payload, @Header("__TypeId__") String eventType) {
        processEvent(payload, eventType);
    }

    @RabbitHandler
    public void handlePasswordResetEvent(PasswordResetEvent payload, @Header("__TypeId__") String eventType) {
        processEvent(payload, eventType);
    }

    @RabbitHandler
    public void handlePasswordResetSuccessfulEvent(PasswordResetSuccessfulEvent payload, @Header("__TypeId__") String eventType) {
        processEvent(payload, eventType);
    }

    @RabbitHandler
    public void handleWelcomeEvent(WelcomeEvent payload, @Header("__TypeId__") String eventType) {
        processEvent(payload, eventType);
    }

    @SuppressWarnings("unchecked")
    private <T> void processEvent(T payload, String eventType) {
        logReceivedEvent(eventType, payload);

        String key = normalizeEventType(eventType, payload);
        try {
            EventHandler<T> handler = (EventHandler<T>) eventHandlers.get(key);
            if (handler != null) {
                handler.handleEvent(payload);
                log.debug("Successfully processed event of type: {}", key);
            } else {
                log.warn("No handler found for event type: {}. Available: {}", key, eventHandlers.keySet());
            }
        } catch (Exception e) {
            log.error("Error processing event of type {}: {}", key, e.getMessage(), e);
            throw new RuntimeException("Failed to process event of type: " + key, e);
        }
    }

    private String normalizeEventType(String eventType, Object payload) {
        if (eventType == null || eventType.isBlank()) {
            return payload.getClass().getSimpleName();
        }
        int dot = eventType.lastIndexOf('.');
        return dot >= 0 ? eventType.substring(dot + 1) : eventType;
    }

    private void logReceivedEvent(String eventType, Object payload) {
        log.info("Received event of type: {} with payload class: {}", eventType, payload.getClass().getSimpleName());
    }
}
