package com.extractor.unraveldocs.brokers.service;

import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.core.MessageBrokerFactory;
import com.extractor.unraveldocs.brokers.core.MessageBrokerType;
import com.extractor.unraveldocs.brokers.core.MessageResult;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.brokers.messages.EmailNotificationMessage;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for producing email notification messages.
 * Uses Kafka for email sending with reliable delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMessageProducerService {

        private final MessageBrokerFactory messageBrokerFactory;
        private final SanitizeLogging sanitizer;

        /**
         * Queue an email for async sending via Kafka.
         *
         * @param to                Recipient email
         * @param subject           Email subject
         * @param templateName      Template name (without .html)
         * @param templateVariables Variables for the template
         * @return CompletableFuture with the send result
         */
        public CompletableFuture<MessageResult> queueEmail(
                        String to,
                        String subject,
                        String templateName,
                        Map<String, Object> templateVariables) {
                EmailNotificationMessage emailMessage = EmailNotificationMessage.of(
                                to,
                                subject,
                                templateName,
                                templateVariables);

                return sendEmailMessage(emailMessage);
        }

        /**
         * Queue a high priority email (e.g., password reset, OTP) via Kafka.
         *
         * @param to                Recipient email
         * @param subject           Email subject
         * @param templateName      Template name
         * @param templateVariables Variables for the template
         * @return CompletableFuture with the send result
         */
        public CompletableFuture<MessageResult> queueHighPriorityEmail(
                        String to,
                        String subject,
                        String templateName,
                        Map<String, Object> templateVariables) {
                EmailNotificationMessage emailMessage = EmailNotificationMessage.highPriority(
                                to,
                                subject,
                                templateName,
                                templateVariables);

                return sendEmailMessage(emailMessage);
        }

        /**
         * Queue an email with user tracking via Kafka.
         *
         * @param to                Recipient email
         * @param subject           Email subject
         * @param templateName      Template name
         * @param templateVariables Variables for the template
         * @param userId            User ID for tracking
         * @return CompletableFuture with the send result
         */
        public CompletableFuture<MessageResult> queueEmailForUser(
                        String to,
                        String subject,
                        String templateName,
                        Map<String, Object> templateVariables,
                        String userId) {
                EmailNotificationMessage emailMessage = EmailNotificationMessage.of(
                                to,
                                subject,
                                templateName,
                                templateVariables).forUser(userId);

                return sendEmailMessage(emailMessage);
        }

        /**
         * Queue an email message directly via Kafka.
         * Kafka is used for emails for:
         * - Unified message broker infrastructure
         * - High throughput for batch emails
         * - Built-in retry and DLQ support
         *
         * @param emailMessage The email message to send
         * @return CompletableFuture with the send result
         */
        public CompletableFuture<MessageResult> sendEmailMessage(EmailNotificationMessage emailMessage) {
                log.debug("Queueing email to: {}, subject: {} via Kafka",
                        sanitizer.sanitizeLogging(emailMessage.to()),
                        sanitizer.sanitizeLogging(emailMessage.subject()));

                Message<EmailNotificationMessage> message = Message.of(
                                emailMessage,
                                KafkaTopicConfig.TOPIC_EMAILS,
                                emailMessage.to() // Use recipient as key for ordering
                );

                return messageBrokerFactory.<EmailNotificationMessage>getProducer(MessageBrokerType.KAFKA)
                                .send(message)
                                .thenApply(result -> {
                                        if (result.success()) {
                                                log.info("Email queued via Kafka. To: {}, MessageId: {}",
                                                        sanitizer.sanitizeLogging(emailMessage.to()),
                                                        sanitizer.sanitizeLogging(result.messageId()));
                                        } else {
                                                log.warn("Failed to queue email via Kafka. To: {}, Error: {}",
                                                        sanitizer.sanitizeLogging(emailMessage.to()),
                                                        sanitizer.sanitizeLogging(result.errorMessage()));
                                        }
                                        return result;
                                });
        }
}
