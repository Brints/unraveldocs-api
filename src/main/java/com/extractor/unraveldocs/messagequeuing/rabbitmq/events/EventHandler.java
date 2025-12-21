package com.extractor.unraveldocs.messagequeuing.rabbitmq.events;

public interface EventHandler<T> {
    void handleEvent(T event);
    String getEventType();
}
