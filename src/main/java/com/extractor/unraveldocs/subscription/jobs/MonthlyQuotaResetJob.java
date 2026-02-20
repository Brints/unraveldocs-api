package com.extractor.unraveldocs.subscription.jobs;

import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Scheduled job to reset monthly quotas for user subscriptions.
 *
 * This job resets the following quotas monthly:
 * - monthlyDocumentsUploaded: Number of documents uploaded in the current
 * billing period
 * - ocrPagesUsed: Number of OCR pages used in the current billing period
 *
 * Storage usage (storageUsed) is NOT reset as it represents cumulative storage
 * consumption.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyQuotaResetJob {

    private final UserSubscriptionRepository userSubscriptionRepository;

    /**
     * Reset monthly quotas at midnight on the first day of each month.
     * Cron expression: "0 0 0 1 * *" = At 00:00:00 on day 1 of every month
     *
     * This job also runs every hour to catch any subscriptions that need reset
     * (in case the server was down on the 1st of the month).
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour to check for quota resets
    @Transactional
    public void resetMonthlyQuotas() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        log.info("Starting monthly quota reset check at {}", now);

        try {
            // Find subscriptions that need quota reset
            List<UserSubscription> subscriptionsToReset = userSubscriptionRepository
                    .findSubscriptionsNeedingQuotaReset(now);

            if (subscriptionsToReset.isEmpty()) {
                log.debug("No subscriptions need quota reset at this time");
                return;
            }

            int resetCount = 0;
            for (UserSubscription subscription : subscriptionsToReset) {
                try {
                    resetSubscriptionQuotas(subscription);
                    resetCount++;
                } catch (Exception e) {
                    log.error("Failed to reset quotas for subscription {}: {}",
                            subscription.getId(), e.getMessage(), e);
                }
            }

            log.info("Monthly quota reset completed. Reset {} subscriptions", resetCount);

        } catch (Exception e) {
            log.error("Failed to process monthly quota reset: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize quota reset dates for subscriptions that don't have one.
     * This runs at startup and periodically to ensure all subscriptions have a
     * reset date.
     */
    @Scheduled(cron = "0 30 0 * * *") // Run daily at 00:30 to initialize any new subscriptions
    @Transactional
    public void initializeQuotaResetDates() {
        log.info("Initializing quota reset dates for subscriptions without one");

        try {
            List<UserSubscription> subscriptionsWithoutResetDate = userSubscriptionRepository
                    .findSubscriptionsWithoutQuotaResetDate();

            if (subscriptionsWithoutResetDate.isEmpty()) {
                log.debug("All subscriptions have quota reset dates");
                return;
            }

            OffsetDateTime nextResetDate = calculateNextResetDate();

            for (UserSubscription subscription : subscriptionsWithoutResetDate) {
                subscription.setQuotaResetDate(nextResetDate);
                userSubscriptionRepository.save(subscription);
            }

            log.info("Initialized quota reset date for {} subscriptions", subscriptionsWithoutResetDate.size());

        } catch (Exception e) {
            log.error("Failed to initialize quota reset dates: {}", e.getMessage(), e);
        }
    }

    /**
     * Reset quotas for a single subscription.
     */
    private void resetSubscriptionQuotas(UserSubscription subscription) {
        String userId = subscription.getUser() != null ? subscription.getUser().getId() : "unknown";

        int previousDocuments = subscription.getMonthlyDocumentsUploaded() != null
                ? subscription.getMonthlyDocumentsUploaded()
                : 0;
        int previousOcrPages = subscription.getOcrPagesUsed() != null
                ? subscription.getOcrPagesUsed()
                : 0;
        int previousAiOps = subscription.getAiOperationsUsed() != null
                ? subscription.getAiOperationsUsed()
                : 0;

        // Reset monthly quotas
        subscription.setMonthlyDocumentsUploaded(0);
        subscription.setOcrPagesUsed(0);
        subscription.setAiOperationsUsed(0);

        // Set next reset date to first of next month
        subscription.setQuotaResetDate(calculateNextResetDate());

        userSubscriptionRepository.save(subscription);

        log.info("Reset quotas for user {}: documents {} -> 0, OCR pages {} -> 0, AI ops {} -> 0, next reset: {}",
                userId, previousDocuments, previousOcrPages, previousAiOps, subscription.getQuotaResetDate());
    }

    /**
     * Calculate the next quota reset date (first day of next month at midnight
     * UTC).
     */
    private OffsetDateTime calculateNextResetDate() {
        return OffsetDateTime.now(ZoneOffset.UTC)
                .with(TemporalAdjusters.firstDayOfNextMonth())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
