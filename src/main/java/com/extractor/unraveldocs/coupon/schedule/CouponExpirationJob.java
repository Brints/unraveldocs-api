package com.extractor.unraveldocs.coupon.schedule;

import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.service.CouponNotificationService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to notify admins and recipients about expiring coupons.
 * Runs daily at 9 AM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpirationJob {

    private final CouponRepository couponRepository;
    private final CouponNotificationService couponNotificationService;
    private final SanitizeLogging sanitizer;

    /**
     * Checks for coupons expiring in 7, 3, and 1 days and sends notifications.
     */
    @Scheduled(cron = "${coupon.expiration.check.cron:0 0 9 * * *}")
    @Transactional
    public void checkExpiringCoupons() {
        log.info("Starting coupon expiration check job");

        OffsetDateTime now = OffsetDateTime.now();

        // Check coupons expiring in 7 days
        checkExpiringInDays(now, 7);

        // Check coupons expiring in 3 days
        checkExpiringInDays(now, 3);

        // Check coupons expiring in 1 day
        checkExpiringInDays(now, 1);

        log.info("Coupon expiration check job completed");
    }

    private void checkExpiringInDays(OffsetDateTime now, int days) {
        OffsetDateTime expiryDate = now.plusDays(days);
        List<Coupon> expiringCoupons = couponRepository.findCouponsExpiringBetween(now, expiryDate);

        log.info("Found {} coupons expiring in {} days", sanitizer.sanitizeLoggingInteger(expiringCoupons.size()), sanitizer.sanitizeLoggingInteger(days));

        for (Coupon coupon : expiringCoupons) {
            try {
                // Send notifications to admins and recipients
                couponNotificationService.sendExpirationNotifications(coupon, days);

                // Mark as notified (only mark once)
                if (days == 1 && !coupon.isExpiryNotificationSent()) {
                    coupon.setExpiryNotificationSent(true);
                    couponRepository.save(coupon);
                }
            } catch (Exception e) {
                log.error("Failed to send expiration notifications for coupon: {}", sanitizer.sanitizeLogging(coupon.getCode()), e);
            }
        }
    }
}
