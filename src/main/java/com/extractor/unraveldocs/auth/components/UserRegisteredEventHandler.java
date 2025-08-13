package com.extractor.unraveldocs.auth.components;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.events.EventHandler;
import com.extractor.unraveldocs.messaging.emailtemplates.AuthEmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventHandler implements EventHandler<UserRegisteredEvent> {

    private final SanitizeLogging sanitizeLogging;
    private final AuthEmailTemplateService authEmailTemplateService;

    @Override
    public void handleEvent(UserRegisteredEvent event) {
        log.info("Processing UserRegisteredEvent for email: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));

        try {
            authEmailTemplateService.sendVerificationEmail(
                event.getEmail(),
                event.getFirstName(),
                event.getLastName(),
                event.getVerificationToken(),
                event.getExpiration()
            );
            log.info("Sent verification email to: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", sanitizeLogging.sanitizeLogging(event.getEmail()), e.getMessage(), e);
        }
    }

    @Override
    public String getEventType() {
        return "UserRegistered";
    }
}
