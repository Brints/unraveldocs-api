package com.extractor.unraveldocs.exceptions.custom;

public class EventProcessingException extends RuntimeException {
    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
