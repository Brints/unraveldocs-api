package com.extractor.unraveldocs.payment.paystack.exception;

/**
 * Exception thrown when there's an error processing Paystack webhooks
 */
public class PaystackWebhookException extends RuntimeException {

    public PaystackWebhookException(String message) {
        super(message);
    }

    public PaystackWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}

