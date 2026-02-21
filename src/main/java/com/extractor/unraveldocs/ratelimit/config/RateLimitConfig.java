package com.extractor.unraveldocs.ratelimit.config;

import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for per-tier AI endpoint rate limits.
 * Loaded from application.properties under the "rate-limit" prefix.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private boolean enabled = true;

    /**
     * Per-tier rate limit settings.
     * Defaults are applied if not overridden in properties.
     */
    private Map<String, TierLimit> tiers = new HashMap<>();

    @Getter
    @Setter
    public static class TierLimit {
        private int requestsPerMinute;
        private int burstCapacity;
        private int dailyLimit;
    }

    /**
     * Get the rate limit for a given subscription plan.
     * Falls back to FREE tier defaults if the plan is not configured.
     */
    public TierLimit getTierLimit(SubscriptionPlans plan) {
        if (plan == null) {
            return getDefaultFreeTier();
        }

        String tierKey = resolveTierKey(plan);
        TierLimit limit = tiers.get(tierKey);
        return limit != null ? limit : getDefaultFreeTier();
    }

    private String resolveTierKey(SubscriptionPlans plan) {
        return switch (plan) {
            case STARTER_MONTHLY, STARTER_YEARLY -> "starter";
            case PRO_MONTHLY, PRO_YEARLY -> "pro";
            case BUSINESS_MONTHLY, BUSINESS_YEARLY -> "business";
            default -> "free";
        };
    }

    private TierLimit getDefaultFreeTier() {
        TierLimit defaultTier = new TierLimit();
        defaultTier.setRequestsPerMinute(5);
        defaultTier.setBurstCapacity(8);
        defaultTier.setDailyLimit(50);
        return defaultTier;
    }
}
