package com.extractor.unraveldocs.brokers.service;

import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.core.MessageBrokerFactory;
import com.extractor.unraveldocs.brokers.core.MessageBrokerType;
import com.extractor.unraveldocs.brokers.core.MessageResult;
import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.brokers.messages.EmailNotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for producing email notification messages.
 * Uses RabbitMQ for email sending (reliable delivery for transactional emails).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMessageProducerService {
    
    private final MessageBrokerFactory messageBrokerFactory;
    
    /**
     * Queue an email for async sending via RabbitMQ.
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param templateName Template name (without .html)
     * @param templateVariables Variables for the template
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> queueEmail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> templateVariables
    ) {
        EmailNotificationMessage emailMessage = EmailNotificationMessage.of(
                to,
                subject,
                templateName,
                templateVariables
        );
        
        return sendEmailMessage(emailMessage);
    }
    
    /**
     * Queue a high priority email (e.g., password reset, OTP) via RabbitMQ.
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param templateName Template name
     * @param templateVariables Variables for the template
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> queueHighPriorityEmail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> templateVariables
    ) {
        EmailNotificationMessage emailMessage = EmailNotificationMessage.highPriority(
                to,
                subject,
                templateName,
                templateVariables
        );
        
        return sendEmailMessage(emailMessage);
    }
    
    /**
     * Queue an email with user tracking via RabbitMQ.
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param templateName Template name
     * @param templateVariables Variables for the template
     * @param userId User ID for tracking
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> queueEmailForUser(
            String to,
            String subject,
            String templateName,
            Map<String, Object> templateVariables,
            String userId
    ) {
        EmailNotificationMessage emailMessage = EmailNotificationMessage.of(
                to,
                subject,
                templateName,
                templateVariables
        ).forUser(userId);
        
        return sendEmailMessage(emailMessage);
    }
    
    /**
     * Queue an email message directly via RabbitMQ.
     * RabbitMQ is used for emails due to:
     * - Reliable delivery with publisher confirms
     * - Transactional email requirements
     * - Lower latency for single messages
     *
     * @param emailMessage The email message to send
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> sendEmailMessage(EmailNotificationMessage emailMessage) {
        log.debug("Queueing email to: {}, subject: {} via RabbitMQ", emailMessage.to(), emailMessage.subject());
        
        // Use RabbitMQ routing key format
        Message<EmailNotificationMessage> message = Message.of(
                emailMessage,
                RabbitMQQueueConfig.EMAIL_ROUTING_KEY,
                emailMessage.to() // Use recipient as key for ordering
        );
        
        return messageBrokerFactory.<EmailNotificationMessage>getProducer(MessageBrokerType.RABBITMQ)
                .send(message)
                .thenApply(result -> {
                    if (result.success()) {
                        log.info("Email queued via RabbitMQ. To: {}, MessageId: {}",
                                emailMessage.to(), result.messageId());
                    } else {
                        log.warn("Failed to queue email via RabbitMQ. To: {}, Error: {}",
                                emailMessage.to(), result.errorMessage());
                    }
                    return result;
                });
    }
}

