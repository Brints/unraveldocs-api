package com.extractor.unraveldocs.payment.common.events;

/**
 * Types of payment events for Kafka message routing and processing.
 */
public enum PaymentEventType {

    // Payment lifecycle events
    PAYMENT_INITIATED,
    PAYMENT_PROCESSING,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_CANCELED,
    PAYMENT_EXPIRED,

    // Subscription events
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_ACTIVATED,
    SUBSCRIPTION_RENEWED,
    SUBSCRIPTION_CANCELED,
    SUBSCRIPTION_EXPIRED,
    SUBSCRIPTION_PAUSED,
    SUBSCRIPTION_RESUMED,
    SUBSCRIPTION_PLAN_CHANGED,

    // Refund events
    REFUND_INITIATED,
    REFUND_SUCCEEDED,
    REFUND_FAILED,
    PARTIAL_REFUND_SUCCEEDED,

    // Dispute/Chargeback events
    DISPUTE_CREATED,
    DISPUTE_WON,
    DISPUTE_LOST,

    // Webhook events (raw from providers)
    WEBHOOK_RECEIVED,

    // Internal events
    PAYMENT_RETRY_SCHEDULED,
    PAYMENT_RECONCILIATION_NEEDED
}

