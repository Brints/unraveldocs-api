package com.extractor.unraveldocs.subscription.dto.response;

import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPlansData {
    private String id;
    private SubscriptionPlans planName;
    private BigDecimal planPrice;
    private SubscriptionCurrency planCurrency;
    private BillingIntervalUnit billingIntervalUnit;
    private Integer billingIntervalValue;
    private Integer documentUploadLimit;
    private Integer ocrPageLimit;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
