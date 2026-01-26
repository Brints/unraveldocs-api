package com.extractor.unraveldocs.coupon.enums;

/**
 * Enum representing the different categories of recipients that can be targeted
 * for coupon distribution.
 */
public enum RecipientCategory {
    /**
     * All users with active paid subscriptions (Individual, Team, or Enterprise)
     */
    ALL_PAID_USERS,

    /**
     * Users with Individual subscription plan only
     */
    INDIVIDUAL_PLAN,

    /**
     * Users with Team subscription plan only
     */
    TEAM_PLAN,

    /**
     * Users with Enterprise subscription plan only
     */
    ENTERPRISE_PLAN,

    /**
     * Free tier users with 20+ OCR operations in last 6 months (upgrade incentive)
     */
    FREE_TIER_ACTIVE,

    /**
     * Users with subscriptions expired within 3 months (reactivation campaign)
     */
    EXPIRED_SUBSCRIPTION,

    /**
     * Recently registered users (onboarding incentive)
     */
    NEW_USERS,

    /**
     * Users exceeding defined usage thresholds (engagement reward)
     */
    HIGH_ACTIVITY_USERS,

    /**
     * Specific individual users targeted via coupon_recipients table
     */
    SPECIFIC_USERS
}
