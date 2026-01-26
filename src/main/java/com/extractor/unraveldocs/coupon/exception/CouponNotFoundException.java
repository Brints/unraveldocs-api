package com.extractor.unraveldocs.coupon.exception;

/**
 * Exception thrown when a coupon is not found.
 */
public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(String message) {
        super(message);
    }

    public CouponNotFoundException(String couponCode, boolean isCode) {
        super(isCode ? "Coupon with code '" + couponCode + "' not found"
                : "Coupon with ID '" + couponCode + "' not found");
    }
}
