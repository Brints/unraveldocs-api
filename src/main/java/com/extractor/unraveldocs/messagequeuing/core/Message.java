package com.extractor.unraveldocs.messagequeuing.core;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic message wrapper for all broker types.
 * Provides a consistent interface for messages regardless of the underlying broker.
 *
 * @param <T> The type of the message payload
 * @param id Unique message identifier
 * @param payload The actual message content
 * @param topic The destination topic/queue name
 * @param key Optional partition/routing key
 * @param headers Additional message headers
 * @param timestamp Message creation timestamp
 */
public record Message<T>(
        String id,
        T payload,
        String topic,
        String key,
        Map<String, String> headers,
        Instant timestamp
) {
    
    /**
     * Creates a new message with auto-generated ID and current timestamp.
     *
     * @param payload The message content
     * @param topic The destination topic
     * @return A new Message instance
     */
    public static <T> Message<T> of(T payload, String topic) {
        return new Message<>(
                UUID.randomUUID().toString(),
                payload,
                topic,
                null,
                Map.of(),
                Instant.now()
        );
    }
    
    /**
     * Creates a new message with a partition key.
     *
     * @param payload The message content
     * @param topic The destination topic
     * @param key The partition/routing key
     * @return A new Message instance
     */
    public static <T> Message<T> of(T payload, String topic, String key) {
        return new Message<>(
                UUID.randomUUID().toString(),
                payload,
                topic,
                key,
                Map.of(),
                Instant.now()
        );
    }
    
    /**
     * Creates a new message with headers.
     *
     * @param payload The message content
     * @param topic The destination topic
     * @param key The partition/routing key
     * @param headers Additional headers
     * @return A new Message instance
     */
    public static <T> Message<T> of(T payload, String topic, String key, Map<String, String> headers) {
        return new Message<>(
                UUID.randomUUID().toString(),
                payload,
                topic,
                key,
                headers != null ? Map.copyOf(headers) : Map.of(),
                Instant.now()
        );
    }
    
    /**
     * Creates a copy of this message with a new topic.
     *
     * @param newTopic The new destination topic
     * @return A new Message instance with the updated topic
     */
    public Message<T> withTopic(String newTopic) {
        return new Message<>(id, payload, newTopic, key, headers, timestamp);
    }
    
    /**
     * Creates a copy of this message with a new key.
     *
     * @param newKey The new partition/routing key
     * @return A new Message instance with the updated key
     */
    public Message<T> withKey(String newKey) {
        return new Message<>(id, payload, topic, newKey, headers, timestamp);
    }
}
