package com.extractor.unraveldocs.user.components;

import com.extractor.unraveldocs.messagequeuing.rabbitmq.events.EventHandler;
import com.extractor.unraveldocs.messagequeuing.rabbitmq.events.EventTypes;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.user.events.UserDeletionScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionScheduledEventHandler implements EventHandler<UserDeletionScheduledEvent> {

    private final UserEmailTemplateService userEmailTemplateService;

    @Override
    public void handleEvent(UserDeletionScheduledEvent event) {
        log.info("Processing UserDeletionScheduledEvent for email: {}", event.getEmail());

        try {
            userEmailTemplateService.scheduleUserDeletion(
                event.getEmail(),
                event.getFirstName(),
                event.getLastName(),
                event.getDeletionDate()
            );
            log.info("Sent account deletion scheduled email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send account deletion scheduled email to {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.USER_DELETION_SCHEDULED;
    }
}
