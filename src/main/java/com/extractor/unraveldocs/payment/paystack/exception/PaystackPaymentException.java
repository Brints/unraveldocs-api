package com.extractor.unraveldocs.payment.paystack.exception;

/**
 * Custom exception for Paystack payment-related errors
 */
public class PaystackPaymentException extends RuntimeException {

    public PaystackPaymentException(String message) {
        super(message);
    }

    public PaystackPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}

