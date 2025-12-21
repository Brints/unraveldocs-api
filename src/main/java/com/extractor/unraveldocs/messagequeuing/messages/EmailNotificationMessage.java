package com.extractor.unraveldocs.messagequeuing.messages;

import java.time.Instant;
import java.util.Map;

/**
 * Message for email notification events.
 * Used to queue email sending operations for async processing.
 *
 * @param to Recipient email address
 * @param subject Email subject
 * @param templateName Thymeleaf template name (without .html)
 * @param templateVariables Variables to pass to the template
 * @param priority Email priority (HIGH, NORMAL, LOW)
 * @param requestedAt When the email was requested
 * @param userId Optional user ID for tracking
 * @param correlationId Optional ID for correlating with other events
 */
public record EmailNotificationMessage(
        String to,
        String subject,
        String templateName,
        Map<String, Object> templateVariables,
        EmailPriority priority,
        Instant requestedAt,
        String userId,
        String correlationId
) {
    
    public enum EmailPriority {
        HIGH, NORMAL, LOW
    }
    
    /**
     * Create a normal priority email message.
     */
    public static EmailNotificationMessage of(
            String to,
            String subject,
            String templateName,
            Map<String, Object> templateVariables
    ) {
        return new EmailNotificationMessage(
                to,
                subject,
                templateName,
                templateVariables,
                EmailPriority.NORMAL,
                Instant.now(),
                null,
                null
        );
    }
    
    /**
     * Create a high priority email message (e.g., password reset).
     */
    public static EmailNotificationMessage highPriority(
            String to,
            String subject,
            String templateName,
            Map<String, Object> templateVariables
    ) {
        return new EmailNotificationMessage(
                to,
                subject,
                templateName,
                templateVariables,
                EmailPriority.HIGH,
                Instant.now(),
                null,
                null
        );
    }
    
    /**
     * Create with user tracking.
     */
    public EmailNotificationMessage forUser(String userId) {
        return new EmailNotificationMessage(
                to,
                subject,
                templateName,
                templateVariables,
                priority,
                requestedAt,
                userId,
                correlationId
        );
    }
    
    /**
     * Create with correlation ID for event tracking.
     */
    public EmailNotificationMessage withCorrelationId(String correlationId) {
        return new EmailNotificationMessage(
                to,
                subject,
                templateName,
                templateVariables,
                priority,
                requestedAt,
                userId,
                correlationId
        );
    }
}
