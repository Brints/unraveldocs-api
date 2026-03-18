package com.extractor.unraveldocs.pushnotification.jobs;

import com.extractor.unraveldocs.pushnotification.config.NotificationConfig;
import com.extractor.unraveldocs.pushnotification.repository.DeviceTokenRepository;
import com.extractor.unraveldocs.pushnotification.repository.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled cleanup for stale and inactive push device tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class StaleTokenCleanupJob {

    private static final int STALE_ACTIVE_DAYS = 90;

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;
    private final NotificationConfig notificationConfig;

    /**
     * Run daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupStaleTokens() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime inactiveCutoff = now.minusDays(notificationConfig.getTokenRetentionDays());
        OffsetDateTime staleActiveCutoff = now.minusDays(STALE_ACTIVE_DAYS);

        int deletedInactive = deviceTokenRepository.deleteInactiveOlderThan(inactiveCutoff);
        int deactivatedStaleActive = deviceTokenRepository.deactivateStaleActiveTokens(staleActiveCutoff);

        int deletedPushDisabled = 0;
        List<String> pushDisabledUserIds = notificationPreferencesRepository.findUserIdsWithPushDisabled();
        for (String userId : pushDisabledUserIds) {
            deletedPushDisabled += deviceTokenRepository.deleteAllByUserId(userId);
        }

        log.info(
                "Completed stale token cleanup: deletedInactive={}, deactivatedStaleActive={}, deletedPushDisabled={}, tokenRetentionDays={}",
                deletedInactive,
                deactivatedStaleActive,
                deletedPushDisabled,
                notificationConfig.getTokenRetentionDays());
    }
}

