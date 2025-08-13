package com.extractor.unraveldocs.events;

public interface EventHandler<T> {
    void handleEvent(T event);
    String getEventType();
}
