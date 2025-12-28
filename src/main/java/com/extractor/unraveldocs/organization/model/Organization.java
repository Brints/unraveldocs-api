package com.extractor.unraveldocs.organization.model;

import com.extractor.unraveldocs.organization.datamodel.OrganizationSubscriptionType;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "organizations", indexes = {
        @Index(columnList = "org_code", unique = true),
        @Index(columnList = "created_by_id"),
        @Index(columnList = "is_active"),
        @Index(columnList = "is_closed"),
        @Index(columnList = "subscription_type")
})
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "org_code", nullable = false, unique = true)
    private String orgCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    private OrganizationSubscriptionType subscriptionType;

    // Organization-specific subscription (separate from personal subscription)
    @Column(name = "payment_gateway_customer_id")
    private String paymentGatewayCustomerId;

    @Column(name = "payment_gateway_subscription_id")
    private String paymentGatewaySubscriptionId;

    @Column(name = "payment_gateway")
    private String paymentGateway; // "stripe" or "paystack"

    // Free trial (10 days)
    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    @Column(name = "has_used_trial", nullable = false)
    private boolean hasUsedTrial = false;

    // Status flags
    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "is_closed", nullable = false)
    private boolean isClosed = false;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    // Creator/Owner reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    // Limits
    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 10;

    @Column(name = "monthly_document_limit")
    private Integer monthlyDocumentLimit; // null for ENTERPRISE (unlimited)

    @Column(name = "monthly_document_upload_count", nullable = false)
    private Integer monthlyDocumentUploadCount = 0;

    @Column(name = "document_count_reset_at")
    private OffsetDateTime documentCountResetAt;

    // Members relationship
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<OrganizationMember> members = new HashSet<>();

    // Invitations relationship
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<OrganizationInvitation> invitations = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    // Helper methods
    public boolean isEnterprise() {
        return subscriptionType == OrganizationSubscriptionType.ENTERPRISE;
    }

    public boolean isPremium() {
        return subscriptionType == OrganizationSubscriptionType.PREMIUM;
    }

    public boolean isInTrial() {
        return trialEndsAt != null && OffsetDateTime.now().isBefore(trialEndsAt);
    }

    public boolean hasTrialExpired() {
        return trialEndsAt != null && OffsetDateTime.now().isAfter(trialEndsAt);
    }

    public boolean canUploadDocument() {
        if (isEnterprise()) {
            return true; // Unlimited for Enterprise
        }
        if (monthlyDocumentLimit == null) {
            return true;
        }
        return monthlyDocumentUploadCount < monthlyDocumentLimit;
    }

    public void incrementDocumentCount() {
        this.monthlyDocumentUploadCount++;
    }

    public void resetMonthlyDocumentCount() {
        this.monthlyDocumentUploadCount = 0;
        this.documentCountResetAt = OffsetDateTime.now();
    }
}
