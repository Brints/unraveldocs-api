package com.extractor.unraveldocs.auth.components;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.brokers.service.EmailMessageProducerService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.brokers.kafka.events.EventHandler;
import com.extractor.unraveldocs.brokers.kafka.events.EventTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventHandler implements EventHandler<UserRegisteredEvent> {

    private final EmailMessageProducerService emailMessageProducerService;
    private final SanitizeLogging sanitizer;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void handleEvent(UserRegisteredEvent event) {
        log.info("Processing UserRegisteredEvent for email: {}", sanitizer.sanitizeLogging(event.getEmail()));

        try {
            String verificationUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/auth/verify-email")
                    .queryParam("email", event.getEmail())
                    .queryParam("token", event.getVerificationToken())
                    .toUriString();

            Map<String, Object> templateVariables = Map.of(
                    "firstName", event.getFirstName(),
                    "lastName", event.getLastName(),
                    "verificationUrl", verificationUrl,
                    "expiration", event.getExpiration());

            emailMessageProducerService.queueEmail(
                    event.getEmail(),
                    "Verify your email address",
                    "emailVerificationToken",
                    templateVariables);
            log.info("Sent verification email to: {}", sanitizer.sanitizeLogging(event.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}",
                    sanitizer.sanitizeLogging(event.getEmail()),
                    e.getMessage(), e);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.USER_REGISTERED;
    }
}
