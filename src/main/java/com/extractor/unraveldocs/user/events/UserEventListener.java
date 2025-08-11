package com.extractor.unraveldocs.user.events;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.events.BaseEvent;
import com.extractor.unraveldocs.messaging.emailtemplates.AuthEmailTemplateService;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.utils.imageupload.aws.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "user.events.queue")
public class UserEventListener {

    private final SanitizeLogging sanitizeLogging;
    private final AuthEmailTemplateService authEmailTemplateService;
    private final UserEmailTemplateService userEmailTemplateService;
    private final AwsS3Service awsS3Service;

    @RabbitHandler
    public void handleUserRegisteredEvent(BaseEvent<UserRegisteredEvent> baseEvent) {
        UserRegisteredEvent event = baseEvent.getPayload();
        log.info("Received UserRegisteredEvent for email: {}. CorrelationId: {}",
                sanitizeLogging.sanitizeLogging(event.getEmail()), baseEvent.getMetadata().getCorrelationId());
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
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    @RabbitHandler
    public void handleUserDeletionScheduledEvent(BaseEvent<UserDeletionScheduledEvent> baseEvent) {
        UserDeletionScheduledEvent event = baseEvent.getPayload();
        log.info("Received UserDeletionScheduledEvent for email: {}. CorrelationId: {}", event.getEmail(), baseEvent.getMetadata().getCorrelationId());
        try {
            userEmailTemplateService.scheduleUserDeletion(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getLastName(),
                    event.getDeletionDate());
        } catch (Exception e) {
            log.error("Error processing UserDeletionScheduledEvent for email {}: {}", event.getEmail(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    @RabbitHandler
    public void handleUserDeletedEvent(BaseEvent<UserDeletedEvent> baseEvent) {
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
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    @RabbitHandler
    public void handlePasswordChangedEvent(BaseEvent<PasswordChangedEvent> baseEvent) {
        PasswordChangedEvent event = baseEvent.getPayload();
        log.info("Received PasswordChangedEvent for email: {}. CorrelationId: {}", event.getEmail(), baseEvent.getMetadata().getCorrelationId());
        try {
            userEmailTemplateService.sendSuccessfulPasswordChange(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getLastName()
            );
            log.info("Sent password change confirmation email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Error processing PasswordChangedEvent for email {}: {}", event.getEmail(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    @RabbitHandler
    public void handlePasswordResetRequestedEvent(BaseEvent<PasswordResetRequestedEvent> baseEvent) {
        PasswordResetRequestedEvent event = baseEvent.getPayload();
        log.info("Received PasswordResetRequestedEvent for email: {}. CorrelationId: {}", event.getEmail(), baseEvent.getMetadata().getCorrelationId());
        try {
            userEmailTemplateService.sendPasswordResetToken(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getLastName(),
                    event.getToken(),
                    event.getExpiration()
            );
            log.info("Sent password reset token email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Error processing PasswordResetRequestedEvent for email {}: {}", event.getEmail(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    @RabbitHandler
    public void handlePasswordResetSuccessfulEvent(BaseEvent<PasswordResetSuccessfulEvent> baseEvent) {
        PasswordResetSuccessfulEvent event = baseEvent.getPayload();
        log.info("Received PasswordResetSuccessfulEvent for email: {}. CorrelationId: {}", event.getEmail(), baseEvent.getMetadata().getCorrelationId());
        try {
            userEmailTemplateService.sendSuccessfulPasswordReset(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getLastName()
            );
            log.info("Sent successful password reset email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Error processing PasswordResetSuccessfulEvent for email {}: {}", event.getEmail(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknownEvent(Object object) {
        log.warn("Received an unknown event type: {}", object.getClass().getName());
    }
}