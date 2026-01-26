package com.extractor.unraveldocs.coupon.dto;

import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for coupon information.
 * Following the pattern established by ActiveOtpData for consistent response
 * structures.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponData {
    private String id;
    private String code;
    private boolean isCustomCode;
    private String description;
    private RecipientCategory recipientCategory;
    private BigDecimal discountPercentage;
    private BigDecimal minPurchaseAmount;
    private Integer maxUsageCount;
    private Integer maxUsagePerUser;
    private Integer currentUsageCount;
    private boolean isActive;
    private String validFrom;
    private String validUntil;
    private boolean isExpired;
    private boolean isCurrentlyValid;
    private String templateId;
    private String templateName;
    private String createdById;
    private String createdByName;
    private String createdAt;
    private String updatedAt;
}
