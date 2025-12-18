package com.extractor.unraveldocs.payment.paystack.exception;

/**
 * Exception thrown when a Paystack subscription is not found
 */
public class PaystackSubscriptionNotFoundException extends RuntimeException {

    public PaystackSubscriptionNotFoundException(String message) {
        super(message);
    }

    public PaystackSubscriptionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

