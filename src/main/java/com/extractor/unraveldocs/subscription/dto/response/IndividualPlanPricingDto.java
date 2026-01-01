package com.extractor.unraveldocs.subscription.dto.response;

import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for individual subscription plan pricing with currency conversion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualPlanPricingDto {

    private String planId;
    private String planName;
    private String displayName;
    private String billingInterval; // MONTH or YEAR

    /**
     * Price with currency conversion information.
     */
    private ConvertedPrice price;

    // Limits
    private Integer documentUploadLimit;
    private Integer ocrPageLimit;

    // Status
    private boolean isActive;

    // Features (derived from plan type)
    private List<String> features;
}
