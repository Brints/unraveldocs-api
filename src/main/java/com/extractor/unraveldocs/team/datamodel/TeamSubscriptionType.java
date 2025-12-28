package com.extractor.unraveldocs.team.datamodel;

/**
 * Team subscription type identifiers.
 * Pricing and limits are stored in the database (team_subscription_plans
 * table).
 */
public enum TeamSubscriptionType {
    TEAM_PREMIUM,
    TEAM_ENTERPRISE;

    public static TeamSubscriptionType fromString(String name) {
        for (TeamSubscriptionType type : TeamSubscriptionType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown team subscription type: " + name);
    }
}
