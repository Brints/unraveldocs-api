package com.extractor.unraveldocs.messagequeuing.core;

import lombok.Getter;

/**
 * Custom exception for messaging operations.
 * Wraps underlying broker-specific exceptions.
 */
@Getter
public class MessagingException extends RuntimeException {
    
    private final MessageBrokerType brokerType;
    private final String messageId;
    private final String topic;
    
    public MessagingException(String message) {
        super(message);
        this.brokerType = null;
        this.messageId = null;
        this.topic = null;
    }
    
    public MessagingException(String message, Throwable cause) {
        super(message, cause);
        this.brokerType = null;
        this.messageId = null;
        this.topic = null;
    }
    
    public MessagingException(
            String message,
            Throwable cause,
            MessageBrokerType brokerType,
            String messageId,
            String topic
    ) {
        super(message, cause);
        this.brokerType = brokerType;
        this.messageId = messageId;
        this.topic = topic;
    }
    
    public static MessagingException sendFailed(
            MessageBrokerType brokerType,
            String messageId,
            String topic,
            Throwable cause
    ) {
        return new MessagingException(
                String.format("Failed to send message %s to topic %s via %s", messageId, topic, brokerType),
                cause,
                brokerType,
                messageId,
                topic
        );
    }
    
    public static MessagingException receiveFailed(
            MessageBrokerType brokerType,
            String topic,
            Throwable cause
    ) {
        return new MessagingException(
                String.format("Failed to receive message from topic %s via %s", topic, brokerType),
                cause,
                brokerType,
                null,
                topic
        );
    }

}
