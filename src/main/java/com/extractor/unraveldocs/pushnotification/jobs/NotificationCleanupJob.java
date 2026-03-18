package com.extractor.unraveldocs.pushnotification.jobs;

import com.extractor.unraveldocs.pushnotification.config.NotificationConfig;
import com.extractor.unraveldocs.pushnotification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Scheduled cleanup for old notification history.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;
    private final NotificationConfig notificationConfig;

    /**
     * Run weekly at 3 AM on Sunday.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void cleanupOldNotifications() {
        int retentionDays = notificationConfig.getNotificationRetentionDays();
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(retentionDays);

        int deleted = notificationRepository.deleteOlderThan(cutoffDate);

        log.info("Completed notification cleanup: deleted={}, retentionDays={}", deleted, retentionDays);
    }
}

