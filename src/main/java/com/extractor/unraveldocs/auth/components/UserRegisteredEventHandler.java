package com.extractor.unraveldocs.auth.components;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.messaging.config.EmailRabbitMQConfig;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.events.EventHandler;
import com.extractor.unraveldocs.events.EventTypes;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailtemplates.AuthEmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventHandler implements EventHandler<UserRegisteredEvent> {

    private final AuthEmailTemplateService authEmailTemplateService;
    private final RabbitTemplate rabbitTemplate;
    private final SanitizeLogging sanitizeLogging;


    @Override
    public void handleEvent(UserRegisteredEvent event) {
        log.info("Processing UserRegisteredEvent for email: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));

        try {
            EmailMessage emailMessage = authEmailTemplateService.prepareVerificationEmail(
                event.getEmail(),
                event.getFirstName(),
                event.getLastName(),
                event.getVerificationToken(),
                event.getExpiration()
            );

            rabbitTemplate.convertAndSend(
                    EmailRabbitMQConfig.EXCHANGE_NAME,
                    EmailRabbitMQConfig.ROUTING_KEY,
                    emailMessage
            );
            log.info("Sent verification email to: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", sanitizeLogging.sanitizeLogging(event.getEmail()), e.getMessage(), e);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.USER_REGISTERED;
    }
}
