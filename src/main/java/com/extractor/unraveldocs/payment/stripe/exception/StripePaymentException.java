package com.extractor.unraveldocs.payment.stripe.exception;

/**
 * Custom exception for Stripe payment-related errors
 */
public class StripePaymentException extends RuntimeException {
    
    public StripePaymentException(String message) {
        super(message);
    }
    
    public StripePaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
