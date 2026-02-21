package com.extractor.unraveldocs.ratelimit.service;

import com.extractor.unraveldocs.exceptions.custom.TooManyRequestsException;
import com.extractor.unraveldocs.ratelimit.config.RateLimitConfig;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing per-user rate limiting using Bucket4j token bucket
 * algorithm.
 * <p>
 * Each user gets two buckets:
 * <ul>
 * <li><b>Per-minute bucket</b>: Sustained rate cap with burst allowance</li>
 * <li><b>Daily bucket</b>: Hard daily cap, refills every 24 hours</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitConfig rateLimitConfig;

    /**
     * Cache of per-user rate limit buckets.
     * Key format: "{userId}:minute" or "{userId}:daily"
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Attempt to consume a token for the given user.
     * Checks both the per-minute and daily buckets.
     *
     * @param userId the authenticated user's ID
     * @param plan   the user's current subscription plan (null = FREE)
     * @return the number of remaining tokens in the per-minute bucket
     * @throws TooManyRequestsException if either bucket is exhausted
     */
    public long consumeToken(String userId, SubscriptionPlans plan) {
        if (!rateLimitConfig.isEnabled()) {
            return Long.MAX_VALUE;
        }

        RateLimitConfig.TierLimit tierLimit = rateLimitConfig.getTierLimit(plan);

        Bucket minuteBucket = buckets.computeIfAbsent(
                userId + ":minute",
                key -> createMinuteBucket(tierLimit));

        Bucket dailyBucket = buckets.computeIfAbsent(
                userId + ":daily",
                key -> createDailyBucket(tierLimit));

        // Check daily limit first
        if (!dailyBucket.tryConsume(1)) {
            log.warn("User {} exceeded daily AI request limit ({})", userId, tierLimit.getDailyLimit());
            throw new TooManyRequestsException(
                    String.format("Daily AI request limit of %d exceeded. Limit resets in 24 hours.",
                            tierLimit.getDailyLimit()));
        }

        // Check per-minute limit
        if (!minuteBucket.tryConsume(1)) {
            // Refund the daily token since the minute limit blocked us
            dailyBucket.addTokens(1);
            log.warn("User {} exceeded per-minute AI request limit ({})", userId, tierLimit.getRequestsPerMinute());
            throw new TooManyRequestsException(
                    String.format("Rate limit exceeded. Maximum %d AI requests per minute. Please try again shortly.",
                            tierLimit.getRequestsPerMinute()));
        }

        long remaining = minuteBucket.getAvailableTokens();
        log.debug("Rate limit check passed for user {}: {} tokens remaining", userId, remaining);
        return remaining;
    }

    /**
     * Get the per-minute limit for a given plan (used for response headers).
     */
    public int getLimit(SubscriptionPlans plan) {
        return rateLimitConfig.getTierLimit(plan).getRequestsPerMinute();
    }

    /**
     * Creates a per-minute bucket with burst capacity.
     * Capacity = burstCapacity (allows short bursts),
     * Refill = requestsPerMinute tokens per minute (sustained rate).
     */
    private Bucket createMinuteBucket(RateLimitConfig.TierLimit tierLimit) {
        Bandwidth bandwidth = Bandwidth.classic(
                tierLimit.getBurstCapacity(),
                Refill.greedy(tierLimit.getRequestsPerMinute(), Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(bandwidth).build();
    }

    /**
     * Creates a daily bucket.
     * Capacity = dailyLimit, refills fully every 24 hours.
     */
    private Bucket createDailyBucket(RateLimitConfig.TierLimit tierLimit) {
        Bandwidth bandwidth = Bandwidth.classic(
                tierLimit.getDailyLimit(),
                Refill.greedy(tierLimit.getDailyLimit(), Duration.ofDays(1)));
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
