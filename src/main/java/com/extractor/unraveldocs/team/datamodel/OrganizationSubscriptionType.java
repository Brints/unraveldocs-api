package com.extractor.unraveldocs.team.datamodel;

import lombok.Getter;

@Getter
public enum OrganizationSubscriptionType {
    PREMIUM("Premium"),
    ENTERPRISE("Enterprise");

    private final String displayName;

    OrganizationSubscriptionType(String displayName) {
        this.displayName = displayName;
    }

    public static OrganizationSubscriptionType fromString(String name) {
        for (OrganizationSubscriptionType type : OrganizationSubscriptionType.values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown subscription type: " + name);
    }
}
