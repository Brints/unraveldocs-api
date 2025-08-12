package com.extractor.unraveldocs.user.components;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.events.EventHandler;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.user.events.PasswordChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordChangedEventHandler implements EventHandler<PasswordChangedEvent> {

    private final SanitizeLogging sanitizeLogging;
    private final UserEmailTemplateService userEmailTemplateService;

    @Override
    public void handleEvent(PasswordChangedEvent event) {
        log.info("Processing PasswordChangedEvent for email: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));
        try {
            userEmailTemplateService.sendSuccessfulPasswordChange(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getLastName()
            );
            log.info("Sent password changed notification email to: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send password changed notification email to {}: {}", sanitizeLogging.sanitizeLogging(event.getEmail()), e.getMessage(), e);
        }
    }

    @Override
    public String getEventType() {
        return "PasswordChanged";
    }
}
