package com.extractor.unraveldocs.brokers.rabbitmq.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * RabbitMQ error handler with Dead Letter Queue (DLQ) support.
 * Routes failed messages to appropriate DLQ exchanges for later analysis or retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMQErrorHandler {
    
    private final RabbitTemplate rabbitTemplate;
    
    private static final String HEADER_ORIGINAL_EXCHANGE = "x-original-exchange";
    private static final String HEADER_ORIGINAL_ROUTING_KEY = "x-original-routing-key";
    private static final String HEADER_EXCEPTION_MESSAGE = "x-exception-message";
    private static final String HEADER_EXCEPTION_CLASS = "x-exception-class";
    private static final String HEADER_FAILURE_TIMESTAMP = "x-failure-timestamp";
    private static final String HEADER_RETRY_COUNT = "x-retry-count";
    
    /**
     * Route a failed message to its Dead Letter Queue.
     *
     * @param originalMessage The failed message
     * @param originalExchange Original exchange the message was sent to
     * @param originalRoutingKey Original routing key
     * @param exception The exception that caused the failure
     */
    public void routeToDlq(
            Message originalMessage,
            String originalExchange,
            String originalRoutingKey,
            Exception exception
    ) {
        String dlqExchange = deriveDlqExchange(originalExchange);
        String dlqRoutingKey = deriveDlqRoutingKey(originalRoutingKey);
        
        log.warn("Routing failed message to DLQ. Original: {}/{}, DLQ: {}/{}",
                originalExchange, originalRoutingKey, dlqExchange, dlqRoutingKey);
        
        try {
            MessageProperties props = originalMessage.getMessageProperties();
            
            // Add DLQ-specific headers
            props.setHeader(HEADER_ORIGINAL_EXCHANGE, originalExchange);
            props.setHeader(HEADER_ORIGINAL_ROUTING_KEY, originalRoutingKey);
            props.setHeader(HEADER_EXCEPTION_MESSAGE, 
                    exception.getMessage() != null ? exception.getMessage() : "Unknown error");
            props.setHeader(HEADER_EXCEPTION_CLASS, exception.getClass().getName());
            props.setHeader(HEADER_FAILURE_TIMESTAMP, Instant.now().toString());
            
            rabbitTemplate.send(dlqExchange, dlqRoutingKey, originalMessage);
            
            log.debug("Successfully routed message to DLQ: {}/{}", dlqExchange, dlqRoutingKey);
            
        } catch (Exception e) {
            log.error("Critical: Failed to route message to DLQ: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retry a message from the DLQ to its original exchange.
     *
     * @param dlqMessage The DLQ message to retry
     * @return true if the message was successfully re-queued
     */
    public boolean retryFromDlq(Message dlqMessage) {
        MessageProperties props = dlqMessage.getMessageProperties();
        
        String originalExchange = (String) props.getHeader(HEADER_ORIGINAL_EXCHANGE);
        String originalRoutingKey = (String) props.getHeader(HEADER_ORIGINAL_ROUTING_KEY);
        
        if (originalExchange == null || originalRoutingKey == null) {
            log.error("Cannot retry message: original exchange/routing key headers missing");
            return false;
        }
        
        int currentRetryCount = getRetryCount(props);
        
        log.info("Retrying message from DLQ. Original: {}/{}, Retry count: {}",
                originalExchange, originalRoutingKey, currentRetryCount + 1);
        
        try {
            // Increment retry count
            props.setHeader(HEADER_RETRY_COUNT, currentRetryCount + 1);
            
            // Remove DLQ-specific headers before retrying
            props.getHeaders().remove(HEADER_EXCEPTION_MESSAGE);
            props.getHeaders().remove(HEADER_EXCEPTION_CLASS);
            props.getHeaders().remove(HEADER_FAILURE_TIMESTAMP);
            
            rabbitTemplate.send(originalExchange, originalRoutingKey, dlqMessage);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to retry message from DLQ: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the retry count from message headers.
     */
    private int getRetryCount(MessageProperties props) {
        Object retryCount = props.getHeader(HEADER_RETRY_COUNT);
        if (retryCount instanceof Integer) {
            return (Integer) retryCount;
        }
        return 0;
    }
    
    /**
     * Derive DLQ exchange name from original exchange.
     */
    private String deriveDlqExchange(String originalExchange) {
        if (originalExchange == null || originalExchange.isBlank()) {
            return "dlq.exchange";
        }
        return originalExchange + ".dlx";
    }
    
    /**
     * Derive DLQ routing key from original routing key.
     */
    private String deriveDlqRoutingKey(String originalRoutingKey) {
        if (originalRoutingKey == null || originalRoutingKey.isBlank()) {
            return "dlq";
        }
        return originalRoutingKey;
    }
}
