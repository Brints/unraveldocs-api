package com.extractor.unraveldocs.subscription.dto.response;

import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for subscription plan with pricing in user's currency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPricingResponse {

    private String planId;
    private String planName;
    private String planDisplayName;
    private String billingInterval; // MONTH or YEAR

    // Original USD pricing
    private ConvertedPrice price;

    // Plan limits
    private Integer documentUploadLimit;
    private Integer ocrPageLimit;

    // Plan features
    private boolean isActive;
    private String description;
}
