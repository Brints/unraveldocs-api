package com.extractor.unraveldocs.coupon.service;

import com.extractor.unraveldocs.coupon.dto.request.ApplyCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.CouponValidationResponse;
import com.extractor.unraveldocs.coupon.dto.response.DiscountCalculationData;
import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.user.model.User;

import java.math.BigDecimal;

/**
 * Service interface for coupon validation and application.
 */
public interface CouponValidationService {

    /**
     * Validates a coupon code for a specific user.
     * Checks: existence, active status, date range, usage limits, recipient
     * eligibility.
     */
    CouponValidationResponse validateCoupon(String couponCode, User user);

    /**
     * Validates coupon and calculates the discount for a given amount.
     */
    DiscountCalculationData applyCouponToAmount(ApplyCouponRequest request, User user);

    /**
     * Records a successful coupon usage after payment.
     */
    void recordCouponUsage(
            Coupon coupon,
            User user,
            BigDecimal originalAmount,
            BigDecimal finalAmount,
            String paymentReference,
            String subscriptionPlan);

    /**
     * Checks if a user is eligible for a specific coupon based on recipient
     * category.
     */
    boolean isUserEligibleForCoupon(User user, Coupon coupon);

    /**
     * Checks if user has exceeded per-user usage limit for a coupon.
     */
    boolean hasExceededPerUserLimit(String couponId, String userId, int maxUsagePerUser);
}
