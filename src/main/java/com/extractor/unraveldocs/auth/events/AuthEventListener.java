package com.extractor.unraveldocs.auth.events;

import com.extractor.unraveldocs.config.RabbitMQConfig;
import com.extractor.unraveldocs.events.BaseEvent;
import com.extractor.unraveldocs.events.EventMetadata;
import com.extractor.unraveldocs.messaging.emailtemplates.AuthEmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {
    private final AuthEmailTemplateService authEmailTemplateService;

    @RabbitListener(queues = RabbitMQConfig.USER_EVENTS_QUEUE_NAME)
    public void handleUserRegisteredEvent(BaseEvent<UserRegisteredEvent> baseEvent) {
        UserRegisteredEvent event = baseEvent.getPayload();
        EventMetadata metadata = baseEvent.getMetadata();

        log.info("Received UserRegisteredEvent for email: {}. CorrelationId: {}", event.getEmail(), metadata.getCorrelationId());

        try {
            authEmailTemplateService.sendVerificationEmail(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getLastName(),
                    event.getVerificationToken(),
                    event.getExpiration()
            );
            log.info("Sent verification email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Error processing UserRegisteredEvent for email: {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }
}
