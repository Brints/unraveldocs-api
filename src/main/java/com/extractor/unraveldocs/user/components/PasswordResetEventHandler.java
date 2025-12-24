package com.extractor.unraveldocs.user.components;

import com.extractor.unraveldocs.brokers.rabbitmq.events.EventHandler;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventTypes;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.user.events.PasswordResetEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEventHandler implements EventHandler<PasswordResetEvent> {

    private final UserEmailTemplateService userEmailTemplateService;

    @Override
    public void handleEvent(PasswordResetEvent event) {
        log.info("Processing PasswordResetEvent for email: {}", event.getEmail());

        try {
            userEmailTemplateService.sendPasswordResetToken(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getLastName(),
                    event.getToken(),
                    event.getExpiration());
            log.info("Sent password reset email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.PASSWORD_RESET_REQUESTED;
    }
}
