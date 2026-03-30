package com.extractor.unraveldocs.admin.dto.request;

import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustSubscriptionDto {
    @NotNull(message = "Plan name is required")
    private SubscriptionPlans plan;

    @NotNull(message = "Billing interval unit is required")
    private BillingIntervalUnit billingIntervalUnit;

    @NotNull(message = "Billing interval value is required")
    private Integer billingIntervalValue;

    @NotNull(message = "Auto-renew flag is required")
    private Boolean autoRenew;

    private String source; // e.g., 'ADMIN_OVERRIDE'
}
