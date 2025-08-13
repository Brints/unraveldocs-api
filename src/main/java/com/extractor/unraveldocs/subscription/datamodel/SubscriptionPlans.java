package com.extractor.unraveldocs.subscription.datamodel;

import lombok.Getter;

@Getter
public enum SubscriptionPlans {
    FREE("Free"),
    BASIC_MONTHLY("Basic_Monthly"),
    BASIC_YEARLY("Basic_Yearly"),
    PREMIUM_MONTHLY("Premium_Monthly"),
    PREMIUM_YEARLY("Premium_Yearly"),
    ENTERPRISE_MONTHLY("Enterprise_Monthly"),
    ENTERPRISE_YEARLY("Enterprise_Yearly");

    private final String planName;

    SubscriptionPlans(String planName) {
        this.planName = planName;
    }

    public static SubscriptionPlans fromString(String planName) {
        for (SubscriptionPlans plan : SubscriptionPlans.values()) {
            if (plan.getPlanName().equalsIgnoreCase(planName)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("No enum constant with name: " + planName);
    }
}
