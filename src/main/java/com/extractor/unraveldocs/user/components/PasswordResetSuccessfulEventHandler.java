package com.extractor.unraveldocs.user.components;

import com.extractor.unraveldocs.events.EventHandler;
import com.extractor.unraveldocs.events.EventTypes;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.user.events.PasswordResetSuccessfulEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetSuccessfulEventHandler implements EventHandler<PasswordResetSuccessfulEvent> {

    private final UserEmailTemplateService userEmailTemplateService;

    @Override
    public void handleEvent(PasswordResetSuccessfulEvent event) {
        log.info("Processing PasswordResetSuccessfulEvent for email: {}", event.getEmail());
        try {
            userEmailTemplateService.sendSuccessfulPasswordReset(
                event.getEmail(),
                event.getFirstName(),
                event.getLastName()
            );
            log.info("Sent password reset success notification email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset success notification email to {}: {}", event.getEmail(), e.getMessage(), e);
        }
        log.info("Password reset successful for email: {}", event.getEmail());
    }

    @Override
    public String getEventType() {
        return EventTypes.PASSWORD_RESET_SUCCESSFUL;
    }
}
