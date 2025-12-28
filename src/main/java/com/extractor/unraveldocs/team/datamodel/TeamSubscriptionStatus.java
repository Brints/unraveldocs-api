package com.extractor.unraveldocs.team.datamodel;

import lombok.Getter;

/**
 * Status of a team subscription.
 */
@Getter
public enum TeamSubscriptionStatus {
    TRIAL("Trial", "Team is in 10-day free trial period"),
    ACTIVE("Active", "Subscription is active and paid"),
    CANCELLED("Cancelled", "Subscription cancelled but active until period ends"),
    EXPIRED("Expired", "Trial or subscription period has ended"),
    PAST_DUE("Past Due", "Payment failed, grace period active");

    private final String displayName;
    private final String description;

    TeamSubscriptionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isAccessAllowed() {
        return this == TRIAL || this == ACTIVE || this == CANCELLED || this == PAST_DUE;
    }

    public static TeamSubscriptionStatus fromString(String name) {
        for (TeamSubscriptionStatus status : TeamSubscriptionStatus.values()) {
            if (status.name().equalsIgnoreCase(name) || status.displayName.equalsIgnoreCase(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown subscription status: " + name);
    }
}
