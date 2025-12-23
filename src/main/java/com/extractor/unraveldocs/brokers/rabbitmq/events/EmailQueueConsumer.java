package com.extractor.unraveldocs.brokers.rabbitmq.events;

import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailQueueConsumer {
    private final EmailOrchestratorService emailOrchestratorService;
    private final SanitizeLogging sanitizer;

    @RabbitListener(queues = RabbitMQQueueConfig.EMAIL_QUEUE)
    public void handleEmailRequest(EmailMessage emailMessage) {
        log.info("Received email request for: {}", sanitizer.sanitizeLogging(emailMessage.getTo()));
        try {
            emailOrchestratorService.sendEmail(emailMessage);
            log.info("Email sent to: {}", sanitizer.sanitizeLogging(emailMessage.getTo()));
        } catch (Exception e) {
            log.error(
                    "Failed to send email to {}: {}",
                    sanitizer.sanitizeLogging(emailMessage.getTo()), e.getMessage(), e);
            throw e; // Rethrow to trigger retry or dead-lettering
        }
    }
}
