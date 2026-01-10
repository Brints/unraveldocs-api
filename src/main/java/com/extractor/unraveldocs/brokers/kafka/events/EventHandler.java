package com.extractor.unraveldocs.brokers.kafka.events;

public interface EventHandler<T> {
    void handleEvent(T event);

    String getEventType();
}
