package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Service responsible for OTP cleanup operations:
 * 1. Marks expired OTPs (runs every 5 minutes)
 * 2. Deletes OTPs expired for more than 24 hours (runs daily at 2 AM)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpCleanupServiceImpl {
    private final OtpRepository otpRepository;

    /**
     * Scheduled task to mark expired OTPs as expired.
     * Runs every 5 minutes for timely expiration status updates.
     * Uses bulk update for efficiency.
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void markExpiredOtps() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            int updatedCount = otpRepository.markExpiredOtps(now);

            if (updatedCount > 0) {
                log.info("Marked {} OTP(s) as expired", updatedCount);
            }
        } catch (Exception e) {
            log.error("Error marking expired OTPs: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to delete OTPs that have been expired for more than 24 hours.
     * Runs daily at 1:00 AM to clean up old OTP records.
     * Uses bulk delete for efficiency.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void deleteOldExpiredOtps() {
        try {
            OffsetDateTime threshold = OffsetDateTime.now().minusHours(24);
            int deletedCount = otpRepository.deleteExpiredOtpsOlderThan(threshold);

            if (deletedCount > 0) {
                log.info("Deleted {} expired OTP(s) older than 24 hours", deletedCount);
            }
        } catch (Exception e) {
            log.error("Error deleting old expired OTPs: {}", e.getMessage(), e);
        }
    }
}

