package com.extractor.unraveldocs.messagequeuing.core;

/**
 * Enum defining available message broker types.
 * Allows runtime selection between different messaging providers.
 */
public enum MessageBrokerType {
    
    /**
     * Apache Kafka message broker.
     */
    KAFKA,
    
    /**
     * RabbitMQ message broker.
     */
    RABBITMQ
}
