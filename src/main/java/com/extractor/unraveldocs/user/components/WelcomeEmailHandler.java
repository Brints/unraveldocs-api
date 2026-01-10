package com.extractor.unraveldocs.user.components;

import com.extractor.unraveldocs.brokers.kafka.events.EventHandler;
import com.extractor.unraveldocs.brokers.kafka.events.EventTypes;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.auth.events.WelcomeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeEmailHandler implements EventHandler<WelcomeEvent> {

    private final UserEmailTemplateService userEmailTemplateService;

    /**
     * Handles the WelcomeEvent by sending a welcome email to the user.
     *
     * @param event The WelcomeEvent containing user details.
     */
    @Override
    public void handleEvent(WelcomeEvent event) {
        log.info("Processing WelcomeEvent for email: {}", event.getEmail());
        userEmailTemplateService.sendWelcomeEmail(
                event.getEmail(),
                event.getFirstName(),
                event.getLastName());
        log.info("Sent welcome email to: {}", event.getEmail());
    }

    @Override
    public String getEventType() {
        return EventTypes.WELCOME_EVENT;
    }
}
