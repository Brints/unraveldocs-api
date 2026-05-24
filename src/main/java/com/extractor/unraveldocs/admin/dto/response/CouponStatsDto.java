package com.extractor.unraveldocs.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponStatsDto {
    private long totalCoupons;
    private long activeCoupons;
    private long expiredCoupons;
    private long totalUsages;
    private BigDecimal totalDiscountGiven;
    private long uniqueUsersWhoUsedCoupons;
    private BigDecimal averageDiscountPercentage;
    private long couponsNearExpiry;
    private long couponsAtUsageLimit;
}
