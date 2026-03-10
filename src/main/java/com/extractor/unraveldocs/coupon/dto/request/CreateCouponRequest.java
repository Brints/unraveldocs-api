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
 * Request DTO for creating a new coupon.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCouponRequest {

    /**
     * Optional custom coupon code. If not provided, system will auto-generate one.
     * Must be 3-40 characters, alphanumeric with hyphens allowed.
     */
    @Size(min = 3, max = 40, message = "Custom code must be between 3 and 40 characters")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Custom code can only contain letters, numbers, and hyphens")
    private String customCode;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.01", message = "Discount percentage must be at least 0.01")
    @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    /**
     * Optional minimum purchase amount required to use this coupon.
     */
    @DecimalMin(value = "0.01", message = "Minimum purchase amount must be at least 0.01")
    private BigDecimal minPurchaseAmount;

    @NotNull(message = "Recipient category is required")
    private RecipientCategory recipientCategory;

    /**
     * Required when recipientCategory is SPECIFIC_USERS.
     * List of user IDs to target with this coupon.
     */
    private List<String> specificUserIds;

    /**
     * Maximum total number of times this coupon can be used. Null means unlimited.
     */
    @Min(value = 1, message = "Max usage count must be at least 1")
    private Integer maxUsageCount;

    /**
     * Maximum number of times each user can use this coupon. Default is 1.
     */
    @Min(value = 1, message = "Max usage per user must be at least 1")
    @Builder.Default
    private Integer maxUsagePerUser = 1;

    @FutureOrPresent(message = "Valid from date must be in the present or future")
    private OffsetDateTime validFrom;

    @Future(message = "Valid until date must be in the future")
    private OffsetDateTime validUntil;

    /**
     * Optional duration value for coupon validity (e.g. 5, 13). Used if validUntil
     * is not provided.
     */
    @Min(value = 1, message = "Duration value must be at least 1")
    private Long validDurationValue;

    /**
     * Optional duration unit for coupon validity (e.g. SECONDS, MINUTES, DAYS).
     */
    private java.time.temporal.ChronoUnit validDurationUnit;

    /**
     * Whether to send notifications to eligible recipients. Default is true.
     */
    @Builder.Default
    private Boolean sendNotifications = true;

    /**
     * Optional template ID to create coupon from a template.
     */
    private String templateId;
}
