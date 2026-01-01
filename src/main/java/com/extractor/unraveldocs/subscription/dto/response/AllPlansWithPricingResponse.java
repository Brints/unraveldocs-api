package com.extractor.unraveldocs.subscription.dto.response;

import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO containing all subscription plans with pricing in a specific
 * currency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllPlansWithPricingResponse {

    /**
     * Individual subscription plans with pricing.
     */
    private List<IndividualPlanPricingDto> individualPlans;

    /**
     * Team subscription plans with pricing.
     */
    private List<TeamPlanPricingDto> teamPlans;

    /**
     * The currency in which prices are displayed.
     */
    private SubscriptionCurrency displayCurrency;

    /**
     * When the exchange rates were last updated.
     */
    private OffsetDateTime exchangeRateTimestamp;
}
