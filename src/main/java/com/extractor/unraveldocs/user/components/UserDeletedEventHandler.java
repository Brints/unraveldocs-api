package com.extractor.unraveldocs.user.components;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.events.EventHandler;
import com.extractor.unraveldocs.events.EventTypes;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.user.events.UserDeletedEvent;
import com.extractor.unraveldocs.utils.imageupload.aws.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedEventHandler implements EventHandler<UserDeletedEvent> {

    private final AwsS3Service awsS3Service;
    private final SanitizeLogging sanitizeLogging;
    private final UserEmailTemplateService userEmailTemplateService;

    @Override
    public void handleEvent(UserDeletedEvent event) {
        log.info("Processing UserDeletedEvent for email: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));

        try {
            // Delete user profile image from S3
            if (event.getProfilePictureUrl() != null && !event.getProfilePictureUrl().isEmpty()) {
                awsS3Service.deleteFile(event.getProfilePictureUrl());
                log.info("Deleted profile picture from S3 for user: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));
            } else {
                log.warn("No profile picture URL provided for user: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));
            }

            // Send account deletion confirmation email
            userEmailTemplateService.sendDeletedAccountEmail(event.getEmail());
            log.info("Sent account deletion confirmation email to: {}", sanitizeLogging.sanitizeLogging(event.getEmail()));
        } catch (Exception e) {
            log.error("Failed to process UserDeletedEvent for {}: {}", sanitizeLogging.sanitizeLogging(event.getEmail()), e.getMessage(), e);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.USER_DELETED;
    }
}
