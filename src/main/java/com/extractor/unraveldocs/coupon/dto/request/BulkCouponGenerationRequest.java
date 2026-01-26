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
 * Request DTO for bulk coupon generation via Kafka.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkCouponGenerationRequest {

    /**
     * Optional template ID to use for coupon configuration.
     * If provided, most fields below are ignored and taken from template.
     */
    private String templateId;

    /**
     * Number of coupons to generate. Max 1000 per request.
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000, message = "Maximum quantity is 1000 per request")
    private Integer quantity;

    /**
     * Optional prefix for generated coupon codes (e.g., "SUMMER2026-").
     */
    @Size(max = 20, message = "Code prefix cannot exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "Code prefix can only contain letters, numbers, and hyphens")
    private String codePrefix;

    // The following fields are used only when templateId is not provided

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

    @FutureOrPresent(message = "Valid from date must be in the present or future")
    private OffsetDateTime validFrom;

    @Future(message = "Valid until date must be in the future")
    private OffsetDateTime validUntil;

    /**
     * Whether to send notifications to eligible recipients. Default is true.
     */
    private Boolean sendNotifications = true;
}
