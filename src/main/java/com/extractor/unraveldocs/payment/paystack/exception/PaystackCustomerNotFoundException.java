package com.extractor.unraveldocs.payment.paystack.exception;

/**
 * Exception thrown when a Paystack customer is not found
 */
public class PaystackCustomerNotFoundException extends RuntimeException {

    public PaystackCustomerNotFoundException(String message) {
        super(message);
    }

    public PaystackCustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

