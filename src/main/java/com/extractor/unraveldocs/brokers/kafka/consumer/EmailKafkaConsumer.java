package com.extractor.unraveldocs.brokers.kafka.consumer;

import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.brokers.messages.EmailNotificationMessage;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for email messages.
 * Replaces the RabbitMQ EmailQueueConsumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class EmailKafkaConsumer {

    private final EmailOrchestratorService emailOrchestratorService;
    private final SanitizeLogging sanitizer;

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_EMAILS, groupId = "email-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleEmailRequest(EmailNotificationMessage emailNotificationMessage, Acknowledgment acknowledgment) {
        log.info("Received email request for: {}", sanitizer.sanitizeLogging(emailNotificationMessage.to()));
        try {
            // Convert EmailNotificationMessage to EmailMessage for the orchestrator
            EmailMessage emailMessage = EmailMessage.builder()
                    .to(emailNotificationMessage.to())
                    .subject(emailNotificationMessage.subject())
                    .templateName(emailNotificationMessage.templateName())
                    .templateModel(emailNotificationMessage.templateVariables())
                    .build();

            emailOrchestratorService.sendEmail(emailMessage);
            log.info("Email sent to: {}", sanitizer.sanitizeLogging(emailNotificationMessage.to()));
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error(
                    "Failed to send email to {}: {}",
                    sanitizer.sanitizeLogging(emailNotificationMessage.to()), e.getMessage(), e);
            throw e; // Rethrow to trigger retry/DLQ handling
        }
    }
}
