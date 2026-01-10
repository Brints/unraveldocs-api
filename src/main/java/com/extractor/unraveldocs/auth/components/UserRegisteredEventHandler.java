package com.extractor.unraveldocs.auth.components;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.brokers.service.EmailMessageProducerService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.brokers.kafka.events.EventHandler;
import com.extractor.unraveldocs.brokers.kafka.events.EventTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventHandler implements EventHandler<UserRegisteredEvent> {

    private final EmailMessageProducerService emailMessageProducerService;
    private final SanitizeLogging sanitizeLogging;

    @Override
    public void handleEvent(UserRegisteredEvent event) {
        log.info("Processing UserRegisteredEvent for email: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));

        try {
            Map<String, Object> templateVariables = Map.of(
                    "firstName", event.getFirstName(),
                    "lastName", event.getLastName(),
                    "verificationToken", event.getVerificationToken(),
                    "expiration", event.getExpiration());

            emailMessageProducerService.queueEmail(
                    event.getEmail(),
                    "Verify your email address",
                    "emailVerificationToken",
                    templateVariables);
            log.info("Sent verification email to: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}",
                    sanitizeLogging.sanitizeLogging(event.getEmail()),
                    e.getMessage(), e);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.USER_REGISTERED;
    }
}
