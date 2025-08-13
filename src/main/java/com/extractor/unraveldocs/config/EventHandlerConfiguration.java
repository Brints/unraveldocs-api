package com.extractor.unraveldocs.config;

import com.extractor.unraveldocs.events.EventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class EventHandlerConfiguration {

    @Bean
    public Map<String, EventHandler<?>> eventHandlers(List<EventHandler<?>> handlers) {
        return handlers.stream()
                .collect(Collectors.toMap(EventHandler::getEventType, handler -> handler));
    }
}
