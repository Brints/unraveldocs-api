package com.extractor.unraveldocs.team.model;

import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionType;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "teams", indexes = {
        @Index(columnList = "team_code", unique = true),
        @Index(columnList = "created_by_id"),
        @Index(columnList = "is_active"),
        @Index(columnList = "is_closed"),
        @Index(columnList = "subscription_type"),
        @Index(columnList = "subscription_status"),
        @Index(columnList = "trial_ends_at"),
        @Index(columnList = "next_billing_date")
})
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "team_code", nullable = false, unique = true)
    private String teamCode;

    // Subscription Type
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    private TeamSubscriptionType subscriptionType;

    // Reference to subscription plan (for database-driven pricing)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private TeamSubscriptionPlan plan;

    // Billing Cycle
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private TeamBillingCycle billingCycle = TeamBillingCycle.MONTHLY;

    // Subscription Status
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false)
    private TeamSubscriptionStatus subscriptionStatus = TeamSubscriptionStatus.TRIAL;

    // Payment Gateway Integration
    @Column(name = "payment_gateway")
    private String paymentGateway; // "stripe" or "paystack"

    @Column(name = "payment_gateway_customer_id")
    private String paymentGatewayCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "paystack_subscription_code")
    private String paystackSubscriptionCode;

    // Trial Management
    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    @Column(name = "has_used_trial", nullable = false)
    private boolean hasUsedTrial = false;

    @Column(name = "trial_reminder_sent", nullable = false)
    private boolean trialReminderSent = false;

    // Billing Dates
    @Column(name = "next_billing_date")
    private OffsetDateTime nextBillingDate;

    @Column(name = "last_billing_date")
    private OffsetDateTime lastBillingDate;

    // Auto-renewal and Cancellation
    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = true;

    @Column(name = "cancellation_requested_at")
    private OffsetDateTime cancellationRequestedAt;

    @Column(name = "subscription_ends_at")
    private OffsetDateTime subscriptionEndsAt;

    // Price at time of subscription (for records)
    @Column(name = "subscription_price", precision = 10, scale = 2)
    private BigDecimal subscriptionPrice;

    @Column(name = "currency")
    private String currency = "USD";

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

    // Limits (derived from subscription type but stored for flexibility)
    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 10;

    @Column(name = "monthly_document_limit")
    private Integer monthlyDocumentLimit; // null for ENTERPRISE (unlimited)

    @Column(name = "monthly_document_upload_count", nullable = false)
    private Integer monthlyDocumentUploadCount = 0;

    @Column(name = "document_count_reset_at")
    private OffsetDateTime documentCountResetAt;

    // Members relationship
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<TeamMember> members = new HashSet<>();

    // Invitations relationship
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<TeamInvitation> invitations = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    // Helper methods
    public boolean isEnterprise() {
        return subscriptionType == TeamSubscriptionType.TEAM_ENTERPRISE;
    }

    public boolean isPremium() {
        return subscriptionType == TeamSubscriptionType.TEAM_PREMIUM;
    }

    public boolean isInTrial() {
        return subscriptionStatus == TeamSubscriptionStatus.TRIAL
                && trialEndsAt != null
                && OffsetDateTime.now().isBefore(trialEndsAt);
    }

    public boolean hasTrialExpired() {
        return trialEndsAt != null && OffsetDateTime.now().isAfter(trialEndsAt);
    }

    public boolean isAccessAllowed() {
        return subscriptionStatus.isAccessAllowed() && !isClosed;
    }

    public boolean isCancelled() {
        return subscriptionStatus == TeamSubscriptionStatus.CANCELLED;
    }

    public boolean canUploadDocument() {
        if (!isAccessAllowed()) {
            return false;
        }
        if (isEnterprise() || monthlyDocumentLimit == null) {
            return true; // Unlimited for Enterprise
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

    public int getDaysUntilTrialExpiry() {
        if (trialEndsAt == null) {
            return 0;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(OffsetDateTime.now(), trialEndsAt);
        return Math.max(0, (int) days);
    }
}
