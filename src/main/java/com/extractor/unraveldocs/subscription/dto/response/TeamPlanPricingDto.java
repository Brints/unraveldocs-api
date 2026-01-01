package com.extractor.unraveldocs.subscription.dto.response;

import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for team subscription plan pricing with currency conversion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPlanPricingDto {

    private String planId;
    private String planName;
    private String displayName;
    private String description;

    /**
     * Monthly price with currency conversion.
     */
    private ConvertedPrice monthlyPrice;

    /**
     * Yearly price with currency conversion.
     */
    private ConvertedPrice yearlyPrice;

    // Limits
    private Integer maxMembers;
    private Integer monthlyDocumentLimit;

    // Feature flags
    private boolean hasAdminPromotion;
    private boolean hasEmailInvitations;
    private Integer trialDays;

    // Status
    private boolean isActive;

    // Features (derived from plan type)
    private List<String> features;
}
