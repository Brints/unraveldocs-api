package com.extractor.unraveldocs.organization.dto.response;

import com.extractor.unraveldocs.organization.datamodel.OrganizationSubscriptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    private String id;
    private String name;
    private String description;
    private String orgCode;
    private OrganizationSubscriptionType subscriptionType;
    private String paymentGateway;

    // Trial info
    private boolean inTrial;
    private OffsetDateTime trialEndsAt;

    // Status
    private boolean active;
    private boolean verified;
    private boolean closed;

    // Limits
    private Integer maxMembers;
    private Integer currentMemberCount;
    private Integer monthlyDocumentLimit;
    private Integer monthlyDocumentUploadCount;

    // Ownership
    private String createdById;
    private String createdByName;
    private boolean isOwner;

    // Timestamps
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
}
