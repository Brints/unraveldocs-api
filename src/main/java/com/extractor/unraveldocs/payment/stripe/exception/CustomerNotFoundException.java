package com.extractor.unraveldocs.payment.stripe.exception;

/**
 * Exception thrown when a Stripe customer is not found
 */
public class CustomerNotFoundException extends RuntimeException {
    
    public CustomerNotFoundException(String message) {
        super(message);
    }
    
    public static CustomerNotFoundException forUser(String userId) {
        return new CustomerNotFoundException("Stripe customer not found for user: " + userId);
    }
}
