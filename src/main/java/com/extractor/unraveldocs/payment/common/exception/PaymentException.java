package com.extractor.unraveldocs.payment.common.exception;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import lombok.Getter;

/**
 * Base exception for payment-related errors across all providers.
 * Contains provider information for consistent error handling.
 */
@Getter
public class PaymentException extends RuntimeException {

    private final PaymentGateway provider;
    private final String errorCode;

    public PaymentException(String message) {
        super(message);
        this.provider = null;
        this.errorCode = null;
    }

    public PaymentException(String message, PaymentGateway provider) {
        super(message);
        this.provider = provider;
        this.errorCode = null;
    }

    public PaymentException(String message, PaymentGateway provider, String errorCode) {
        super(message);
        this.provider = provider;
        this.errorCode = errorCode;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.provider = null;
        this.errorCode = null;
    }

    public PaymentException(String message, PaymentGateway provider, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorCode = null;
    }

    public PaymentException(String message, PaymentGateway provider, String errorCode, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorCode = errorCode;
    }
}
