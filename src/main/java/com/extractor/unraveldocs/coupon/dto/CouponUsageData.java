package com.extractor.unraveldocs.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for individual coupon usage record.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponUsageData {
    private String id;
    private String couponId;
    private String couponCode;
    private String userId;
    private String userEmail;
    private String userName;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentReference;
    private String subscriptionPlan;
    private String usedAt;
}
