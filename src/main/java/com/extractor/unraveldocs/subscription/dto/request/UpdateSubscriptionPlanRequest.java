package com.extractor.unraveldocs.subscription.dto.request;

import com.extractor.unraveldocs.subscription.config.SubscriptionCurrencyDeserializer;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UpdateSubscriptionPlanRequest(
        @JsonDeserialize(using = SubscriptionCurrencyDeserializer.class)
        SubscriptionCurrency newPlanCurrency,
        BigDecimal newPlanPrice,
        BillingIntervalUnit billingIntervalUnit,
        Integer billingIntervalValue,
        Integer documentUploadLimit,
        Integer ocrPageLimit
) {
}
