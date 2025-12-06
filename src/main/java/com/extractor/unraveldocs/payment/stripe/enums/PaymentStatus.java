package com.extractor.unraveldocs.payment.stripe.enums;

/**
 * Enum representing the status of a payment transaction
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    REQUIRES_ACTION,
    REQUIRES_PAYMENT_METHOD,
    REQUIRES_CONFIRMATION,
    REFUNDED,
    PARTIALLY_REFUNDED
}
