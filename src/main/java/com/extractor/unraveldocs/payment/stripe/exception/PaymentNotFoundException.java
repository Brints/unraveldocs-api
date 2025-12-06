package com.extractor.unraveldocs.payment.stripe.exception;

/**
 * Exception thrown when a payment is not found
 */
public class PaymentNotFoundException extends RuntimeException {
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
    
    public PaymentNotFoundException(String paymentId, String type) {
        super(String.format("Payment not found with %s: %s", type, paymentId));
    }
}
