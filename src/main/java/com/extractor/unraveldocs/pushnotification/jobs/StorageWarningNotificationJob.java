package com.extractor.unraveldocs.pushnotification.jobs;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.pushnotification.model.StorageWarningSent;
import com.extractor.unraveldocs.pushnotification.repository.StorageWarningSentRepository;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Scheduled job to send storage warning notifications.
 * Checks user storage usage and sends warnings at 80%, 90%, and 95% thresholds.
 * Only active when scheduling is enabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class StorageWarningNotificationJob {

    private final UserSubscriptionRepository subscriptionRepository;
    private final StorageWarningSentRepository warningRepository;
    private final NotificationService notificationService;

    private static final int THRESHOLD_80 = 80;
    private static final int THRESHOLD_90 = 90;
    private static final int THRESHOLD_95 = 95;

    /**
     * Run daily at 10 AM to check storage usage.
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void checkStorageUsage() {
        log.info("Starting storage warning notification job");

        List<UserSubscription> subscriptions = subscriptionRepository.findAll();
        int warningsSent = 0;

        for (UserSubscription subscription : subscriptions) {
            try {
                warningsSent += checkUserStorage(subscription);
            } catch (Exception e) {
                log.error("Error checking storage for subscription {}: {}", subscription.getId(), e.getMessage());
            }
        }

        log.info("Completed storage warning job. Sent {} warnings", warningsSent);
    }

    private int checkUserStorage(UserSubscription subscription) {
        Long storageUsed = subscription.getStorageUsed();
        Long storageLimit = subscription.getPlan().getStorageLimit();

        // Skip if no limit or no usage
        if (storageLimit == null || storageLimit <= 0 || storageUsed == null) {
            return 0;
        }

        User user = subscription.getUser();
        double usagePercent = (storageUsed * 100.0) / storageLimit;
        int warningsSent = 0;

        // Check 95% threshold
        if (usagePercent >= THRESHOLD_95) {
            warningsSent += sendWarningIfNotSent(user, THRESHOLD_95, NotificationType.STORAGE_WARNING_95,
                    "Storage Almost Full!",
                    "You've used 95% of your storage. Please free up space or upgrade your plan.");
        }
        // Check 90% threshold
        else if (usagePercent >= THRESHOLD_90) {
            warningsSent += sendWarningIfNotSent(user, THRESHOLD_90, NotificationType.STORAGE_WARNING_90,
                    "Storage Running Low",
                    "You've used 90% of your storage. Consider upgrading for more space.");
            // Clear higher level warnings if user freed up space
            clearHigherWarnings(user.getId(), THRESHOLD_90);
        }
        // Check 80% threshold
        else if (usagePercent >= THRESHOLD_80) {
            warningsSent += sendWarningIfNotSent(user, THRESHOLD_80, NotificationType.STORAGE_WARNING_80,
                    "Storage Usage Notice",
                    "You've used 80% of your available storage.");
            clearHigherWarnings(user.getId(), THRESHOLD_80);
        }
        // Below 80% - clear all warnings
        else {
            int cleared = warningRepository.deleteAllForUser(user.getId());
            if (cleared > 0) {
                log.debug("Cleared {} storage warnings for user {} (usage at {}%)",
                        cleared, user.getId(), Math.round(usagePercent));
            }
        }

        return warningsSent;
    }

    private int sendWarningIfNotSent(User user, int level, NotificationType type, String title, String message) {
        // Check if warning was already sent
        if (warningRepository.existsByUserIdAndWarningLevel(user.getId(), level)) {
            return 0;
        }

        // Send notification
        Map<String, String> data = Map.of(
                "warningLevel", String.valueOf(level),
                "action", "upgrade_storage");

        notificationService.sendToUser(user.getId(), type, title, message, data);

        // Record that warning was sent
        StorageWarningSent warning = StorageWarningSent.builder()
                .user(user)
                .warningLevel(level)
                .build();
        warningRepository.save(warning);

        log.info("Sent {}% storage warning to user {}", level, user.getId());
        return 1;
    }

    private void clearHigherWarnings(String userId, int currentLevel) {
        warningRepository.deleteAboveLevel(userId, currentLevel);
    }
}
