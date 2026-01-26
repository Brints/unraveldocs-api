package com.extractor.unraveldocs.coupon.exception;

/**
 * Exception thrown when a coupon is invalid or cannot be used.
 */
public class InvalidCouponException extends RuntimeException {

    private final String errorCode;

    public InvalidCouponException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidCouponException(String message) {
        super(message);
        this.errorCode = "INVALID_COUPON";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
