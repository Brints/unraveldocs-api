package com.extractor.unraveldocs.team.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a team's trial is about to expire (3 days before).
 * This event triggers sending a warning email to the team owner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamTrialExpiringEvent {
    private String teamId;
    private String teamName;
    private String teamCode;
    private String ownerEmail;
    private String ownerFirstName;
    private String subscriptionType;
    private String billingCycle;
    private String price;
    private String currency;
    private String trialEndsAt;
    private int daysRemaining;
}
