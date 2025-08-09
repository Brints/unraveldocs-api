package com.extractor.unraveldocs.user.events;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.config.RabbitMQConfig;
import com.extractor.unraveldocs.events.BaseEvent;
import com.extractor.unraveldocs.messaging.emailtemplates.AuthEmailTemplateService;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.utils.imageupload.aws.AwsS3Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {
    private final AuthEmailTemplateService authEmailTemplateService;
    private final UserEmailTemplateService userEmailTemplateService;
    private final AwsS3Service awsS3Service;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.USER_EVENTS_QUEUE_NAME)
    public void handleUserEvents(Message message) {
        String eventType = message.getMessageProperties().getHeader("eventType");
        String correlationId = message.getMessageProperties().getCorrelationId();

        if (eventType == null) {
            log.warn("Received a message without an eventType header. CorrelationId: {}", correlationId);
            return;
        }

        try {
            switch (eventType) {
                case "UserRegistered" -> handleUserRegisteredEvent(objectMapper.readValue(message.getBody(), new TypeReference<BaseEvent<UserRegisteredEvent>>() {}));
                case "UserDeletionScheduled" -> handleUserDeletionScheduledEvent(objectMapper.readValue(message.getBody(), new TypeReference<BaseEvent<UserDeletionScheduledEvent>>() {}));
                case "UserDeleted" -> handleUserDeletedEvent(objectMapper.readValue(message.getBody(), new TypeReference<BaseEvent<UserDeletedEvent>>() {}));
                default -> log.warn("Unknown eventType '{}' received. CorrelationId: {}", eventType, correlationId);
            }
        } catch (IOException e) {
            log.error("Failed to deserialize event message. CorrelationId: {}, Error: {}", correlationId, e.getMessage(), e);
        }
    }

    private void handleUserRegisteredEvent(BaseEvent<UserRegisteredEvent> baseEvent) {
        UserRegisteredEvent event = baseEvent.getPayload();
        log.info("Received UserRegisteredEvent for email: {}. CorrelationId: {}", event.getEmail(), baseEvent.getMetadata().getCorrelationId());
        try {
            authEmailTemplateService.sendVerificationEmail(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getLastName(),
                    event.getVerificationToken(),
                    event.getExpiration()
            );
        } catch (Exception e) {
            log.error("Error processing UserRegisteredEvent for email {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }

    private void handleUserDeletionScheduledEvent(BaseEvent<UserDeletionScheduledEvent> baseEvent) {
        UserDeletionScheduledEvent event = baseEvent.getPayload();
        log.info("Received UserDeletionScheduledEvent for email: {}. CorrelationId: {}", event.getEmail(), baseEvent.getMetadata().getCorrelationId());
        try {
            userEmailTemplateService.scheduleUserDeletion(event.getEmail(), event.getFirstName(), event.getLastName(), event.getDeletionDate());
        } catch (Exception e) {
            log.error("Error processing UserDeletionScheduledEvent for email {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }

    private void handleUserDeletedEvent(BaseEvent<UserDeletedEvent> baseEvent) {
        UserDeletedEvent event = baseEvent.getPayload();
        log.info("Received UserDeletedEvent for email: {}. CorrelationId: {}", event.getEmail(), baseEvent.getMetadata().getCorrelationId());
        try {
            if (event.getProfilePictureUrl() != null) {
                awsS3Service.deleteFile(event.getProfilePictureUrl());
                log.info("Deleted profile picture for user {}", event.getEmail());
            }
            userEmailTemplateService.sendDeletedAccountEmail(event.getEmail());
        } catch (Exception e) {
            log.error("Error processing UserDeletedEvent for email {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }
}
