package com.extractor.unraveldocs.coupon.service;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.dto.CouponUsageData;
import com.extractor.unraveldocs.coupon.dto.request.CreateCouponRequest;
import com.extractor.unraveldocs.coupon.dto.request.UpdateCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.CouponAnalyticsData;
import com.extractor.unraveldocs.coupon.dto.response.CouponListData;
import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for coupon management operations.
 */
public interface CouponService {

    /**
     * Creates a new coupon.
     */
    UnravelDocsResponse<CouponData> createCoupon(CreateCouponRequest request, User createdBy);

    /**
     * Updates an existing coupon.
     */
    UnravelDocsResponse<CouponData> updateCoupon(String couponId, UpdateCouponRequest request, User updatedBy);

    /**
     * Deactivates a coupon.
     */
    UnravelDocsResponse<Void> deactivateCoupon(String couponId, User user);

    /**
     * Gets a coupon by ID.
     */
    UnravelDocsResponse<CouponData> getCouponById(String couponId);

    /**
     * Gets a coupon by code.
     */
    UnravelDocsResponse<CouponData> getCouponByCode(String code);

    /**
     * Gets all coupons with pagination and optional filtering.
     */
    UnravelDocsResponse<CouponListData> getAllCoupons(
            int page,
            int size,
            Boolean isActive,
            RecipientCategory recipientCategory);

    /**
     * Gets usage statistics for a coupon.
     */
    UnravelDocsResponse<List<CouponUsageData>> getCouponUsage(String couponId, int page, int size);

    /**
     * Gets comprehensive analytics for a coupon.
     */
    UnravelDocsResponse<CouponAnalyticsData> getCouponAnalytics(
            String couponId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Gets all coupons available for a specific user.
     */
    UnravelDocsResponse<List<CouponData>> getCouponsForUser(String userId);

    /**
     * Generates a unique coupon code.
     */
    String generateCouponCode(String prefix);
}
