package com.extractor.unraveldocs.messagequeuing.rabbitmq.producer;

import com.extractor.unraveldocs.messagequeuing.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * RabbitMQ implementation of the MessageProducer interface.
 * Provides async and sync message sending capabilities via RabbitTemplate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMQMessageProducer<T> implements MessageProducer<T> {
    
    private final RabbitTemplate rabbitTemplate;
    
    @Override
    public MessageBrokerType getBrokerType() {
        return MessageBrokerType.RABBITMQ;
    }
    
    @Override
    public CompletableFuture<MessageResult> send(Message<T> message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doSend(message);
            } catch (Exception e) {
                log.error("Failed to send message to exchange/queue: {}", e.getMessage(), e);
                return MessageResult.failure(message.id(), message.topic(), e);
            }
        });
    }
    
    @Override
    public MessageResult sendAndWait(Message<T> message) {
        try {
            return doSend(message);
        } catch (Exception e) {
            log.error("Failed to send message synchronously: {}", e.getMessage(), e);
            throw MessagingException.sendFailed(
                    MessageBrokerType.RABBITMQ,
                    message.id(),
                    message.topic(),
                    e
            );
        }
    }
    
    /**
     * Internal send implementation.
     * The topic is used as the routing key, and we derive the exchange from it.
     */
    private MessageResult doSend(Message<T> message) {
        log.debug("Sending message to RabbitMQ. Topic/RoutingKey: {}, Key: {}, Id: {}",
                message.topic(), message.key(), message.id());
        
        // Determine exchange and routing key from topic
        String exchange = deriveExchange(message.topic());
        String routingKey = message.topic();
        
        rabbitTemplate.convertAndSend(exchange, routingKey, message.payload(), msg -> {
            MessageProperties props = msg.getMessageProperties();
            
            // Add message ID
            props.setMessageId(message.id());
            
            // Add correlation ID (use key if available)
            if (message.key() != null) {
                props.setCorrelationId(message.key());
            }
            
            // Add timestamp
            props.setTimestamp(java.util.Date.from(message.timestamp()));
            
            // Add custom headers
            if (message.headers() != null) {
                message.headers().forEach((key, value) -> {
                    if (value != null) {
                        props.setHeader(key, value);
                    }
                });
            }
            
            return msg;
        });
        
        log.debug("Message sent successfully to RabbitMQ. Exchange: {}, RoutingKey: {}, Id: {}",
                exchange, routingKey, message.id());
        
        return MessageResult.success(message.id(), message.topic());
    }
    
    /**
     * Derive exchange name from topic/routing key.
     * Convention: topic format is "domain.action" -> exchange is "domain.events.exchange"
     */
    private String deriveExchange(String topic) {
        if (topic == null || topic.isBlank()) {
            return ""; // Default exchange
        }
        
        // If topic contains a dot, use first segment as domain
        int dotIndex = topic.indexOf('.');
        if (dotIndex > 0) {
            String domain = topic.substring(0, dotIndex);
            return domain + ".events.exchange";
        }
        
        // Otherwise, treat the whole topic as the exchange
        return topic + ".exchange";
    }
    
    /**
     * Send to a specific exchange with routing key.
     * 
     * @param exchange The exchange name
     * @param routingKey The routing key
     * @param payload The message payload
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> sendToExchange(String exchange, String routingKey, T payload) {
        Message<T> message = Message.of(payload, routingKey);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Sending to exchange: {}, routingKey: {}", exchange, routingKey);
                
                rabbitTemplate.convertAndSend(exchange, routingKey, payload, msg -> {
                    msg.getMessageProperties().setMessageId(message.id());
                    msg.getMessageProperties().setTimestamp(java.util.Date.from(message.timestamp()));
                    return msg;
                });
                
                return MessageResult.success(message.id(), routingKey);
            } catch (Exception e) {
                log.error("Failed to send to exchange {}: {}", exchange, e.getMessage(), e);
                return MessageResult.failure(message.id(), routingKey, e);
            }
        });
    }
}
