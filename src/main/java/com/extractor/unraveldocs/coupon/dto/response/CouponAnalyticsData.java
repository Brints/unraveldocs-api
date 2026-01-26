package com.extractor.unraveldocs.coupon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for comprehensive coupon analytics.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponAnalyticsData {
    private String couponId;
    private String couponCode;

    // Summary statistics
    private int totalUsageCount;
    private int uniqueUsersCount;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalOriginalAmount;
    private BigDecimal totalFinalAmount;
    private BigDecimal revenueImpact;
    private BigDecimal averageDiscountPerTransaction;

    // Rates
    private double redemptionRate;
    private int potentialRecipientsCount;

    // Breakdown by subscription plan
    private Map<String, Integer> usageBySubscriptionPlan;

    // Breakdown by recipient category
    private Map<String, Integer> usageByRecipientCategory;

    // Time-series data (list of daily data points)
    private List<DailyAnalytics> dailyAnalytics;

    // Period info
    private String startDate;
    private String endDate;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyAnalytics {
        private String date;
        private int usageCount;
        private int uniqueUsers;
        private BigDecimal discountAmount;
        private BigDecimal revenueImpact;
    }
}
