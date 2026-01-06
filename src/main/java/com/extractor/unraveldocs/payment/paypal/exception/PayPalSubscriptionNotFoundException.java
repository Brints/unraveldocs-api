package com.extractor.unraveldocs.payment.paypal.exception;

/**
 * Exception thrown when a PayPal subscription is not found.
 */
public class PayPalSubscriptionNotFoundException extends RuntimeException {

    public PayPalSubscriptionNotFoundException(String message) {
        super(message);
    }

    public PayPalSubscriptionNotFoundException(String subscriptionId, boolean byId) {
        super(byId ? "Subscription not found with ID: " + subscriptionId
                : "Subscription not found: " + subscriptionId);
    }
}
