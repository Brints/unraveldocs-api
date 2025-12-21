package com.extractor.unraveldocs.messagequeuing.core;

/**
 * Marker interface and callback definitions for message consumers.
 * Implementations handle incoming messages from the broker.
 *
 * @param <T> The type of message payload this consumer handles
 */
public interface MessageConsumer<T> {
    
    /**
     * Get the broker type this consumer uses.
     *
     * @return The message broker type
     */
    MessageBrokerType getBrokerType();
    
    /**
     * Get the topics this consumer subscribes to.
     *
     * @return Array of topic names
     */
    String[] getTopics();
    
    /**
     * Get the consumer group ID.
     *
     * @return The consumer group identifier
     */
    String getGroupId();
    
    /**
     * Callback interface for processing messages.
     *
     * @param <T> The type of message payload
     */
    @FunctionalInterface
    interface MessageHandler<T> {
        
        /**
         * Handle an incoming message.
         *
         * @param message The received message
         * @throws Exception if processing fails (triggers retry/DLQ)
         */
        void handle(Message<T> message) throws Exception;
    }
    
    /**
     * Callback interface for batch message processing.
     *
     * @param <T> The type of message payload
     */
    @FunctionalInterface
    interface BatchMessageHandler<T> {
        
        /**
         * Handle a batch of incoming messages.
         *
         * @param messages The received messages
         * @throws Exception if processing fails
         */
        void handle(Iterable<Message<T>> messages) throws Exception;
    }
}
