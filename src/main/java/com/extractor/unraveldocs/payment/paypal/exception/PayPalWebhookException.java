package com.extractor.unraveldocs.payment.paypal.exception;

/**
 * Exception thrown for PayPal webhook processing failures.
 */
public class PayPalWebhookException extends RuntimeException {

    public PayPalWebhookException(String message) {
        super(message);
    }

    public PayPalWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
