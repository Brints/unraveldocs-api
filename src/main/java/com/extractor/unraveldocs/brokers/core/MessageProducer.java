package com.extractor.unraveldocs.brokers.core;

import java.util.concurrent.CompletableFuture;

/**
 * Generic message producer interface for all broker implementations.
 * Provides a consistent API for sending messages regardless of the underlying broker.
 *
 * @param <T> The type of message payload this producer handles
 */
public interface MessageProducer<T> {
    
    /**
     * Get the broker type this producer uses.
     *
     * @return The message broker type
     */
    MessageBrokerType getBrokerType();
    
    /**
     * Send a message asynchronously.
     *
     * @param message The message to send
     * @return A CompletableFuture that completes with the send result
     */
    CompletableFuture<MessageResult> send(Message<T> message);
    
    /**
     * Send a message and wait for acknowledgment.
     * This is a blocking operation.
     *
     * @param message The message to send
     * @return The result of the send operation
     * @throws MessagingException if the send fails
     */
    MessageResult sendAndWait(Message<T> message);
    
    /**
     * Send a payload to a topic with minimal configuration.
     * Uses default settings for key and headers.
     *
     * @param payload The message payload
     * @param topic The destination topic
     * @return A CompletableFuture that completes with the send result
     */
    default CompletableFuture<MessageResult> send(T payload, String topic) {
        return send(Message.of(payload, topic));
    }
    
    /**
     * Send a payload to a topic with a partition key.
     *
     * @param payload The message payload
     * @param topic The destination topic
     * @param key The partition/routing key
     * @return A CompletableFuture that completes with the send result
     */
    default CompletableFuture<MessageResult> send(T payload, String topic, String key) {
        return send(Message.of(payload, topic, key));
    }
}
