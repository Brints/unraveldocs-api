package com.extractor.unraveldocs.team.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for team details.
 */
@Data
@Builder
public class TeamResponse {
    private String id;
    private String name;
    private String description;
    private String teamCode;

    // Subscription details
    private String subscriptionType;
    private String billingCycle;
    private String subscriptionStatus;
    private BigDecimal subscriptionPrice;
    private String currency;

    // Status flags
    private boolean isActive;
    private boolean isVerified;
    private boolean isClosed;
    private boolean autoRenew;

    // Dates
    private OffsetDateTime trialEndsAt;
    private OffsetDateTime nextBillingDate;
    private OffsetDateTime subscriptionEndsAt;
    private OffsetDateTime cancellationRequestedAt;
    private OffsetDateTime createdAt;

    // Member info
    private int currentMemberCount;
    private int maxMembers;
    private Integer monthlyDocumentLimit;

    // Context-specific
    private boolean isOwner;
}
