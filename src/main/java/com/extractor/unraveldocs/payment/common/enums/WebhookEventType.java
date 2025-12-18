package com.extractor.unraveldocs.payment.common.enums;

/**
 * Unified webhook event types across all payment providers.
 * Maps provider-specific events to common event types.
 */
public enum WebhookEventType {
    // Payment events
    PAYMENT_CREATED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_CANCELED,
    PAYMENT_REQUIRES_ACTION,
    
    // Refund events
    REFUND_CREATED,
    REFUND_SUCCEEDED,
    REFUND_FAILED,
    
    // Subscription events
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_UPDATED,
    SUBSCRIPTION_CANCELLED,
    SUBSCRIPTION_PAUSED,
    SUBSCRIPTION_RESUMED,
    SUBSCRIPTION_TRIAL_ENDING,
    
    // Invoice events
    INVOICE_CREATED,
    INVOICE_PAID,
    INVOICE_PAYMENT_FAILED,
    INVOICE_UPCOMING,
    
    // Customer events
    CUSTOMER_CREATED,
    CUSTOMER_UPDATED,
    
    // Dispute events
    DISPUTE_CREATED,
    DISPUTE_UPDATED,
    DISPUTE_CLOSED,
    
    // Payment method events
    PAYMENT_METHOD_ATTACHED,
    PAYMENT_METHOD_DETACHED,
    
    // Unknown/unhandled event
    UNKNOWN
}
