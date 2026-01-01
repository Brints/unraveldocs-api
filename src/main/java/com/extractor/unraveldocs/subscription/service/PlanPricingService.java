package com.extractor.unraveldocs.subscription.service;

import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.dto.response.AllPlansWithPricingResponse;
import com.extractor.unraveldocs.subscription.dto.response.SupportedCurrenciesResponse;

/**
 * Service for retrieving subscription plan pricing with currency conversion.
 * Provides methods for frontend to display plans in user's preferred currency.
 */
public interface PlanPricingService {

    /**
     * Get all subscription plans (individual and team) with prices converted to the
     * specified currency.
     *
     * @param currency Target currency for price conversion
     * @return Response containing all plans with converted pricing
     */
    AllPlansWithPricingResponse getAllPlansWithPricing(SubscriptionCurrency currency);

    /**
     * Get list of all supported currencies for frontend dropdown.
     *
     * @return Response containing all supported currencies
     */
    SupportedCurrenciesResponse getSupportedCurrencies();
}
