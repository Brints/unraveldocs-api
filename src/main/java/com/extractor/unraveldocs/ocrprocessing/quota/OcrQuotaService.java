package com.extractor.unraveldocs.ocrprocessing.quota;

import com.extractor.unraveldocs.ocrprocessing.config.OcrProperties;
import com.extractor.unraveldocs.ocrprocessing.metrics.OcrMetrics;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for managing OCR usage quotas per user and tier.
 * Uses Redis for distributed quota tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrQuotaService {

    private static final String QUOTA_KEY_PREFIX = "ocr:quota:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate redisTemplate;
    private final OcrProperties ocrProperties;
    private final OcrMetrics ocrMetrics;

    /**
     * Check if a user has remaining quota for OCR processing.
     *
     * @param userId The user ID
     * @param tier   The user's subscription tier
     * @return true if the user has remaining quota
     */
    public boolean hasRemainingQuota(String userId, String tier) {
        if (!ocrProperties.getQuota().isEnabled()) {
            return true;
        }

        int limit = getDailyLimitForTier(tier);
        if (limit < 0) {
            return true; // Unlimited
        }

        long currentUsage = getCurrentUsage(userId);
        return currentUsage < limit;
    }

    /**
     * Consume one unit of quota for a user.
     *
     * @param userId   The user ID
     * @param tier     The user's subscription tier
     * @param provider The OCR provider being used
     * @return true if quota was successfully consumed, false if exceeded
     */
    public boolean consumeQuota(String userId, String tier, OcrProviderType provider) {
        if (!ocrProperties.getQuota().isEnabled()) {
            return true;
        }

        int limit = getDailyLimitForTier(tier);
        if (limit < 0) {
            // Unlimited tier, still track usage
            incrementUsage(userId);
            ocrMetrics.recordQuotaUsage(userId, tier, provider);
            return true;
        }

        long currentUsage = incrementUsage(userId);

        if (ocrProperties.getQuota().isTrackUsage()) {
            ocrMetrics.recordQuotaUsage(userId, tier, provider);
        }

        if (currentUsage > limit) {
            log.warn("User {} exceeded OCR quota for tier {}. Usage: {}, Limit: {}",
                    userId, tier, currentUsage, limit);
            ocrMetrics.recordQuotaExceeded(userId, tier);
            return false;
        }

        return true;
    }

    /**
     * Get current usage count for a user.
     *
     * @param userId The user ID
     * @return Current usage count
     */
    public long getCurrentUsage(String userId) {
        String key = buildQuotaKey(userId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * Get remaining quota for a user.
     *
     * @param userId The user ID
     * @param tier   The user's subscription tier
     * @return Remaining quota count, or -1 if unlimited
     */
    public long getRemainingQuota(String userId, String tier) {
        int limit = getDailyLimitForTier(tier);
        if (limit < 0) {
            return -1; // Unlimited
        }

        long currentUsage = getCurrentUsage(userId);
        return Math.max(0, limit - currentUsage);
    }

    /**
     * Increment usage counter and return new value.
     */
    private long incrementUsage(String userId) {
        String key = buildQuotaKey(userId);
        Long newValue = redisTemplate.opsForValue().increment(key);

        // Set expiry if this is the first increment of the day
        if (newValue != null && newValue == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }

        return newValue != null ? newValue : 0;
    }

    /**
     * Get the daily limit for a subscription tier.
     */
    private int getDailyLimitForTier(String tier) {
        if (tier == null) {
            tier = "free";
        }

        return switch (tier.toLowerCase()) {
            case "basic" -> ocrProperties.getQuota().getBasicTierDailyLimit();
            case "premium" -> ocrProperties.getQuota().getPremiumTierDailyLimit();
            case "enterprise" -> ocrProperties.getQuota().getEnterpriseTierDailyLimit();
            default -> ocrProperties.getQuota().getFreeTierDailyLimit();
        };
    }

    /**
     * Build Redis key for quota tracking.
     */
    private String buildQuotaKey(String userId) {
        String today = LocalDate.now().format(DATE_FORMAT);
        return QUOTA_KEY_PREFIX + userId + ":" + today;
    }

    /**
     * Reset quota for a user (admin function).
     *
     * @param userId The user ID to reset
     */
    public void resetQuota(String userId) {
        String key = buildQuotaKey(userId);
        redisTemplate.delete(key);
        log.info("Reset OCR quota for user: {}", userId);
    }
}
