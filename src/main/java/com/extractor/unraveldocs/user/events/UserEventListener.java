package com.extractor.unraveldocs.user.events;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.events.EventHandler;
import com.extractor.unraveldocs.exceptions.custom.EventProcessingException;
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
@RabbitListener(queues = "user.events.queue")
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

    @SuppressWarnings("unchecked")
    private <T> void processEvent(T payload, String eventType) {
        logReceivedEvent(eventType, payload);

        try {
            EventHandler<T> handler = (EventHandler<T>) eventHandlers.get(eventType);
            if (handler != null) {
                handler.handleEvent(payload);
                log.debug("Successfully processed event of type: {}", eventType);
            } else {
                log.warn("No handler found for event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event of type {}: {}", eventType, e.getMessage(), e);
            throw new EventProcessingException("Failed to process event of type: " + eventType, e);
        }
    }

    private void logReceivedEvent(String eventType, Object payload) {
        log.info("Received event of type: {} with payload class: {}", eventType, payload.getClass().getSimpleName());
    }
}
