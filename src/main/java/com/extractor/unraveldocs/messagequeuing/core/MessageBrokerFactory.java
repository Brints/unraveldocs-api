package com.extractor.unraveldocs.messagequeuing.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Factory for obtaining the appropriate MessageProducer implementation
 * based on the requested broker type.
 * <p>
 */
@Slf4j
@Component
public class MessageBrokerFactory {
    
    private final Map<MessageBrokerType, MessageProducer<?>> producers;
    private final MessageBrokerType defaultBroker;
    
    public MessageBrokerFactory(List<MessageProducer<?>> messageProducers) {
        this.producers = new EnumMap<>(MessageBrokerType.class);
        
        for (MessageProducer<?> producer : messageProducers) {
            producers.put(producer.getBrokerType(), producer);
            log.info("Registered message producer: {}", producer.getBrokerType());
        }
        
        // Determine default broker (prefer Kafka)
        if (producers.containsKey(MessageBrokerType.KAFKA)) {
            defaultBroker = MessageBrokerType.KAFKA;
        } else if (producers.containsKey(MessageBrokerType.RABBITMQ)) {
            defaultBroker = MessageBrokerType.RABBITMQ;
        } else if (!producers.isEmpty()) {
            defaultBroker = producers.keySet().iterator().next();
        } else {
            defaultBroker = null;
            log.warn("No message producers registered!");
        }
        
        log.info("Default message broker: {}", defaultBroker);
    }
    
    /**
     * Get the message producer for a specific broker type.
     *
     * @param brokerType The broker type to get
     * @param <T> The message payload type
     * @return The message producer
     * @throws IllegalArgumentException if the broker is not supported
     */
    @SuppressWarnings("unchecked")
    public <T> MessageProducer<T> getProducer(MessageBrokerType brokerType) {
        MessageProducer<?> producer = producers.get(brokerType);
        if (producer == null) {
            throw new IllegalArgumentException("Unsupported message broker: " + brokerType);
        }
        return (MessageProducer<T>) producer;
    }
    
    /**
     * Get the message producer if available.
     *
     * @param brokerType The broker type to get
     * @param <T> The message payload type
     * @return Optional containing the producer if available
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<MessageProducer<T>> getProducerIfAvailable(MessageBrokerType brokerType) {
        return Optional.ofNullable((MessageProducer<T>) producers.get(brokerType));
    }
    
    /**
     * Get the default message producer.
     *
     * @param <T> The message payload type
     * @return The default message producer
     * @throws IllegalStateException if no default broker is configured
     */
    @SuppressWarnings("unchecked")
    public <T> MessageProducer<T> getDefaultProducer() {
        if (defaultBroker == null) {
            throw new IllegalStateException("No message producers are registered");
        }
        return (MessageProducer<T>) producers.get(defaultBroker);
    }
    
    /**
     * Get the default broker type.
     *
     * @return The default broker type, or null if none configured
     */
    public MessageBrokerType getDefaultBrokerType() {
        return defaultBroker;
    }
    
    /**
     * Check if a broker type is supported.
     *
     * @param brokerType The broker type to check
     * @return true if the broker is supported
     */
    public boolean isSupported(MessageBrokerType brokerType) {
        return producers.containsKey(brokerType);
    }
    
    /**
     * Get all supported broker types.
     *
     * @return Set of supported broker types
     */
    public Set<MessageBrokerType> getSupportedBrokers() {
        return producers.keySet();
    }
}
