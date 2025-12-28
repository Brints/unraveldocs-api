package com.extractor.unraveldocs.team.datamodel;

import lombok.Getter;

/**
 * Billing cycle for team subscriptions.
 */
@Getter
public enum TeamBillingCycle {
    MONTHLY("Monthly", 1),
    YEARLY("Yearly", 12);

    private final String displayName;
    private final int months;

    TeamBillingCycle(String displayName, int months) {
        this.displayName = displayName;
        this.months = months;
    }

    public static TeamBillingCycle fromString(String name) {
        for (TeamBillingCycle cycle : TeamBillingCycle.values()) {
            if (cycle.name().equalsIgnoreCase(name) || cycle.displayName.equalsIgnoreCase(name)) {
                return cycle;
            }
        }
        throw new IllegalArgumentException("Unknown billing cycle: " + name);
    }
}
