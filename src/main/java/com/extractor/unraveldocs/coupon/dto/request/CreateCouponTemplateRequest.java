package com.extractor.unraveldocs.coupon.dto.request;

import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a coupon template.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCouponTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(min = 3, max = 100, message = "Template name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.01", message = "Discount percentage must be at least 0.01")
    @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    @DecimalMin(value = "0.01", message = "Minimum purchase amount must be at least 0.01")
    private BigDecimal minPurchaseAmount;

    @NotNull(message = "Recipient category is required")
    private RecipientCategory recipientCategory;

    @Min(value = 1, message = "Max usage count must be at least 1")
    private Integer maxUsageCount;

    @Min(value = 1, message = "Max usage per user must be at least 1")
    private Integer maxUsagePerUser = 1;

    @NotNull(message = "Validity days is required")
    @Min(value = 1, message = "Validity days must be at least 1")
    @Max(value = 365, message = "Validity days cannot exceed 365")
    private Integer validityDays;
}
