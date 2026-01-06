package com.extractor.unraveldocs.team.model;

import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Database entity for team subscription plans.
 * Contains pricing, limits, and feature flags.
 */
@Data
@Entity
@Table(name = "team_subscription_plans", indexes = {
        @Index(columnList = "name", unique = true),
        @Index(columnList = "is_active")
})
@NoArgsConstructor
@AllArgsConstructor
public class TeamSubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name; // TEAM_PREMIUM, TEAM_ENTERPRISE

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Pricing
    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "yearly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal yearlyPrice;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    // Limits
    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Column(name = "monthly_document_limit")
    private Integer monthlyDocumentLimit; // NULL = unlimited

    // Features
    @Column(name = "has_admin_promotion", nullable = false)
    private boolean hasAdminPromotion = false;

    @Column(name = "has_email_invitations", nullable = false)
    private boolean hasEmailInvitations = false;

    @Column(name = "trial_days", nullable = false)
    private Integer trialDays = 10;

    @Column(name = "storage_limit")
    private Long storageLimit; // Storage limit in bytes, NULL = unlimited (Enterprise)

    // Payment Gateway Integration
    @Column(name = "stripe_price_id_monthly")
    private String stripePriceIdMonthly;

    @Column(name = "stripe_price_id_yearly")
    private String stripePriceIdYearly;

    @Column(name = "paystack_plan_code_monthly")
    private String paystackPlanCodeMonthly;

    @Column(name = "paystack_plan_code_yearly")
    private String paystackPlanCodeYearly;

    // Status
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    // Helper methods

    /**
     * Get the price based on billing cycle.
     */
    public BigDecimal getPrice(TeamBillingCycle cycle) {
        return cycle == TeamBillingCycle.YEARLY ? yearlyPrice : monthlyPrice;
    }

    /**
     * Get the Stripe price ID based on billing cycle.
     */
    public String getStripePriceId(TeamBillingCycle cycle) {
        return cycle == TeamBillingCycle.YEARLY ? stripePriceIdYearly : stripePriceIdMonthly;
    }

    /**
     * Get the Paystack plan code based on billing cycle.
     */
    public String getPaystackPlanCode(TeamBillingCycle cycle) {
        return cycle == TeamBillingCycle.YEARLY ? paystackPlanCodeYearly : paystackPlanCodeMonthly;
    }

    /**
     * Check if documents are unlimited.
     */
    public boolean hasUnlimitedDocuments() {
        return monthlyDocumentLimit == null;
    }
}
