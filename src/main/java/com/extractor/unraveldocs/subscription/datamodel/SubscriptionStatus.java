package com.extractor.unraveldocs.subscription.datamodel;

import lombok.Getter;

@Getter
public enum SubscriptionStatus {
    ACTIVE("Active"),
    CANCELLED("Cancelled"),
    TRIAL("Trial"),
    EXPIRED("Expired");

    private final String statusName;

    SubscriptionStatus(String statusName) {
        this.statusName = statusName;
    }

    public static SubscriptionStatus fromString(String statusName) {
        for (SubscriptionStatus status : SubscriptionStatus.values()) {
            if (status.getStatusName().equalsIgnoreCase(statusName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No enum constant with name: " + statusName);
    }
}
