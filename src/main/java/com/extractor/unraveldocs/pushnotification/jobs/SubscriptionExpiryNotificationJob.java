package com.extractor.unraveldocs.pushnotification.jobs;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Scheduled job to send subscription expiry notifications.
 * Runs daily to check for upcoming subscription expirations.
 * Only active when scheduling is enabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionExpiryNotificationJob {

    private final UserSubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;
    private final com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService emailTemplateService;

    /**
     * Run daily at 9 AM to check subscription expirations.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void checkSubscriptionExpirations() {
        log.info("Starting subscription expiry notification job");

        OffsetDateTime now = OffsetDateTime.now();

        // Check for subscriptions expiring in 7 days
        checkAndNotify(now.plusDays(7), now.plusDays(8), NotificationType.SUBSCRIPTION_EXPIRING_7_DAYS,
                "Subscription Expiring Soon",
                "Your subscription will expire in 7 days. Renew now to avoid interruption.");

        // Check for subscriptions expiring in 3 days
        checkAndNotify(now.plusDays(3), now.plusDays(4), NotificationType.SUBSCRIPTION_EXPIRING_3_DAYS,
                "Subscription Expiring",
                "Your subscription will expire in 3 days. Please renew to continue using our services.");

        // Check for subscriptions expiring in 1 day
        checkAndNotify(now.plusDays(1), now.plusDays(2), NotificationType.SUBSCRIPTION_EXPIRING_1_DAY,
                "Subscription Expires Tomorrow",
                "Your subscription expires tomorrow! Renew now to avoid losing access.");

        // Check for trials expiring soon (3 days)
        checkTrialExpirations(now);

        log.info("Completed subscription expiry notification job");
    }

    private void checkAndNotify(OffsetDateTime startWindow, OffsetDateTime endWindow,
            NotificationType type, String title, String message) {
        List<UserSubscription> expiringSubscriptions = subscriptionRepository
                .findByCurrentPeriodEndBetweenAndAutoRenewFalse(startWindow, endWindow);

        for (UserSubscription subscription : expiringSubscriptions) {
            try {
                String userId = subscription.getUser().getId();
                String planName = subscription.getPlan().getName().name();

                Map<String, String> data = Map.of(
                        "subscriptionId", subscription.getId(),
                        "planName", planName,
                        "expiryDate", subscription.getCurrentPeriodEnd().toString());

                notificationService.sendToUser(userId, type, title, message, data);
                log.debug("Sent {} notification to user {}", type, userId);
            } catch (Exception e) {
                log.error("Failed to send expiry notification: {}", e.getMessage());
            }
        }

        if (!expiringSubscriptions.isEmpty()) {
            log.info("Sent {} notifications for {}", expiringSubscriptions.size(), type);
        }
    }

    private void checkTrialExpirations(OffsetDateTime now) {
        // Find subscriptions in trial that expire in 3 days
        OffsetDateTime trialEndStart = now.plusDays(3);
        OffsetDateTime trialEndEnd = now.plusDays(4);

        List<UserSubscription> expiringTrials = subscriptionRepository
                .findByTrialEndsAtBetweenAndStatusEquals(trialEndStart, trialEndEnd, "trialing");

        for (UserSubscription subscription : expiringTrials) {
            try {
                String userId = subscription.getUser().getId();

                Map<String, String> data = Map.of(
                        "subscriptionId", subscription.getId(),
                        "trialEndDate", subscription.getTrialEndsAt().toString());

                notificationService.sendToUser(userId, NotificationType.TRIAL_EXPIRING_SOON,
                        "Trial Ending Soon",
                        "Your free trial ends in 3 days. Subscribe now to continue enjoying our services.",
                        data);

                // Send email
                String expiryDate = subscription.getTrialEndsAt()
                        .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG));
                emailTemplateService.sendTrialExpiringSoonEmail(
                        subscription.getUser().getEmail(),
                        subscription.getUser().getFirstName(),
                        subscription.getUser().getLastName(),
                        expiryDate);

                log.debug("Sent trial expiry notification to user {}", userId);
            } catch (Exception e) {
                log.error("Failed to send trial expiry notification: {}", e.getMessage());
            }
        }

        if (!expiringTrials.isEmpty()) {
            log.info("Sent {} trial expiry notifications", expiringTrials.size());
        }
    }
}
