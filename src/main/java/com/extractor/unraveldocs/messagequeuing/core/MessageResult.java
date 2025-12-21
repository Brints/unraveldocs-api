package com.extractor.unraveldocs.messagequeuing.core;

import java.time.Instant;

/**
 * Result of a message send operation.
 * Provides feedback about whether the message was successfully sent.
 *
 * @param success Whether the send operation was successful
 * @param messageId The ID of the sent message
 * @param topic The topic the message was sent to
 * @param partition The partition the message was sent to (for Kafka)
 * @param offset The offset assigned to the message (for Kafka)
 * @param timestamp Timestamp when the message was acknowledged
 * @param errorMessage Error message if the send failed
 */
public record MessageResult(
        boolean success,
        String messageId,
        String topic,
        Integer partition,
        Long offset,
        Instant timestamp,
        String errorMessage
) {
    
    /**
     * Creates a successful result.
     *
     * @param messageId The ID of the sent message
     * @param topic The destination topic
     * @param partition The partition (nullable)
     * @param offset The offset (nullable)
     * @return A successful MessageResult
     */
    public static MessageResult success(String messageId, String topic, Integer partition, Long offset) {
        return new MessageResult(true, messageId, topic, partition, offset, Instant.now(), null);
    }
    
    /**
     * Creates a successful result without partition/offset info.
     *
     * @param messageId The ID of the sent message
     * @param topic The destination topic
     * @return A successful MessageResult
     */
    public static MessageResult success(String messageId, String topic) {
        return new MessageResult(true, messageId, topic, null, null, Instant.now(), null);
    }
    
    /**
     * Creates a failure result.
     *
     * @param messageId The ID of the message that failed to send
     * @param topic The destination topic
     * @param errorMessage Description of the failure
     * @return A failed MessageResult
     */
    public static MessageResult failure(String messageId, String topic, String errorMessage) {
        return new MessageResult(false, messageId, topic, null, null, Instant.now(), errorMessage);
    }
    
    /**
     * Creates a failure result from an exception.
     *
     * @param messageId The ID of the message that failed to send
     * @param topic The destination topic
     * @param exception The exception that caused the failure
     * @return A failed MessageResult
     */
    public static MessageResult failure(String messageId, String topic, Throwable exception) {
        return new MessageResult(
                false,
                messageId,
                topic,
                null,
                null,
                Instant.now(),
                exception.getMessage()
        );
    }
}
