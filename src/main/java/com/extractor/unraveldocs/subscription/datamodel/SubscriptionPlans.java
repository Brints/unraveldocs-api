package com.extractor.unraveldocs.subscription.datamodel;

import lombok.Getter;

@Getter
public enum SubscriptionPlans {
    FREE("Free"),
    STARTER_MONTHLY("Starter_Monthly"),
    STARTER_YEARLY("Starter_Yearly"),
    PRO_MONTHLY("Pro_Monthly"),
    PRO_YEARLY("Pro_Yearly"),
    BUSINESS_MONTHLY("Business_Monthly"),
    BUSINESS_YEARLY("Business_Yearly");

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
