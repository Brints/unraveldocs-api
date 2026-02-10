package com.extractor.unraveldocs.subscription.jobs;

import com.extractor.unraveldocs.subscription.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job to check for expired trials and reset them to the free plan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrialExpiryJob {

    private final UserSubscriptionService userSubscriptionService;

    /**
     * Run every hour to check for expired trials.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkExpiredTrials() {
        log.info("Starting scheduled job: Check Expired Trials");
        try {
            userSubscriptionService.checkAndExpireTrials();
            log.info("Completed scheduled job: Check Expired Trials");
        } catch (Exception e) {
            log.error("Failed to execute scheduled job: Check Expired Trials", e);
        }
    }
}
