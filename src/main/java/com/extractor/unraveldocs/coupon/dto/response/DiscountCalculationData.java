package com.extractor.unraveldocs.coupon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for discount calculation when applying a coupon.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiscountCalculationData {
    private String couponCode;
    private BigDecimal originalAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String currency;

    /**
     * Indicates if there was a minimum purchase requirement that was met.
     */
    private BigDecimal minPurchaseAmount;
    private boolean minPurchaseRequirementMet;
}
