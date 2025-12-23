package com.extractor.unraveldocs.brokers.rabbitmq.config;

import com.extractor.unraveldocs.brokers.rabbitmq.events.EventHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration for RabbitMQ event handlers.
 * Creates a map of event type to handler for dynamic dispatch.
 */
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class EventHandlerConfiguration {

    @Bean
    public Map<String, EventHandler<?>> eventHandlers(List<EventHandler<?>> handlers) {
        return handlers.stream()
                .collect(Collectors.toMap(EventHandler::getEventType, handler -> handler));
    }
}

