package com.extractor.unraveldocs.coupon.dto.request;

import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Request DTO for updating an existing coupon.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCouponRequest {

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Discount percentage must be at least 0.01")
    @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    @DecimalMin(value = "0.01", message = "Minimum purchase amount must be at least 0.01")
    private BigDecimal minPurchaseAmount;

    private RecipientCategory recipientCategory;

    private List<String> specificUserIds;

    @Min(value = 1, message = "Max usage count must be at least 1")
    private Integer maxUsageCount;

    @Min(value = 1, message = "Max usage per user must be at least 1")
    private Integer maxUsagePerUser;

    private OffsetDateTime validFrom;

    private OffsetDateTime validUntil;

    private Boolean isActive;
}
