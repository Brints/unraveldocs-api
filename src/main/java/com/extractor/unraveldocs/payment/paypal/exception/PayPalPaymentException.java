package com.extractor.unraveldocs.payment.paypal.exception;

/**
 * Exception thrown for PayPal payment operation failures.
 */
public class PayPalPaymentException extends RuntimeException {

    private final String errorCode;

    public PayPalPaymentException(String message) {
        super(message);
        this.errorCode = null;
    }

    public PayPalPaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public PayPalPaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PayPalPaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
