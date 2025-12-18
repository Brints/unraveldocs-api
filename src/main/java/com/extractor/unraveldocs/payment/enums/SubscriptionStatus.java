package com.extractor.unraveldocs.payment.enums;

/**
 * Enum representing Stripe subscription statuses
 * Matches Stripe's subscription status values
 */
public enum SubscriptionStatus {
    INCOMPLETE,
    INCOMPLETE_EXPIRED,
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    UNPAID,
    PAUSED
}
