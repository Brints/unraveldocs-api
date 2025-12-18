package com.extractor.unraveldocs.payment.stripe.model;

import com.extractor.unraveldocs.payment.enums.SubscriptionStatus;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Entity for tracking Stripe subscriptions locally.
 * Enables local queries without Stripe API calls and provides
 * subscription lifecycle tracking.
 */
@Data
@Entity
@Builder
@Table(name = "stripe_subscriptions", indexes = {
        @Index(name = "idx_stripe_sub_user_id", columnList = "user_id"),
        @Index(name = "idx_stripe_sub_stripe_id", columnList = "stripe_subscription_id"),
        @Index(name = "idx_stripe_sub_status", columnList = "status"),
        @Index(name = "idx_stripe_sub_customer", columnList = "stripe_customer_id"),
        @Index(name = "idx_stripe_sub_current_period_end", columnList = "current_period_end")
})
@NoArgsConstructor
@AllArgsConstructor
public class StripeSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stripe_customer_id", referencedColumnName = "id", nullable = false)
    private StripeCustomer stripeCustomer;

    @Column(name = "stripe_subscription_id", nullable = false, unique = true)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "price_id")
    private String priceId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "plan_name")
    private String planName;

    @Column
    private Long quantity;

    @Column(length = 3)
    private String currency;

    @Column(name = "current_period_start")
    private OffsetDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "trial_start")
    private OffsetDateTime trialStart;

    @Column(name = "trial_end")
    private OffsetDateTime trialEnd;

    @Column(name = "cancel_at")
    private OffsetDateTime cancelAt;

    @Column(name = "canceled_at")
    private OffsetDateTime canceledAt;

    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "default_payment_method_id")
    private String defaultPaymentMethodId;

    @Column(name = "latest_invoice_id")
    private String latestInvoiceId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
