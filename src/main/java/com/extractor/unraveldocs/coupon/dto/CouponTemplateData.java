package com.extractor.unraveldocs.coupon.dto;

import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for coupon template information.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponTemplateData {
    private String id;
    private String name;
    private String description;
    private BigDecimal discountPercentage;
    private BigDecimal minPurchaseAmount;
    private RecipientCategory recipientCategory;
    private Integer maxUsageCount;
    private Integer maxUsagePerUser;
    private Integer validityDays;
    private boolean isActive;
    private int couponsCreatedCount;
    private String createdById;
    private String createdByName;
    private String createdAt;
    private String updatedAt;
}
