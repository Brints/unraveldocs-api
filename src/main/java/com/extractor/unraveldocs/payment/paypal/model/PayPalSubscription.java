package com.extractor.unraveldocs.payment.paypal.model;

import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity for tracking PayPal subscriptions.
 */
@Data
@Entity
@Builder
@Table(name = "paypal_subscriptions", indexes = {
        @Index(name = "idx_paypal_subscription_id", columnList = "subscription_id"),
        @Index(name = "idx_paypal_subscription_user_id", columnList = "user_id"),
        @Index(name = "idx_paypal_subscription_status", columnList = "status"),
        @Index(name = "idx_paypal_subscription_plan_id", columnList = "plan_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class PayPalSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paypal_customer_id", referencedColumnName = "id")
    private PayPalCustomer paypalCustomer;

    /**
     * PayPal Subscription ID.
     */
    @Column(name = "subscription_id", unique = true, nullable = false)
    private String subscriptionId;

    /**
     * PayPal Plan ID.
     */
    @Column(name = "plan_id", nullable = false)
    private String planId;

    /**
     * Subscription status: APPROVAL_PENDING, APPROVED, ACTIVE, SUSPENDED,
     * CANCELLED, EXPIRED.
     */
    @Column(nullable = false)
    private String status;

    /**
     * Amount per billing cycle.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    /**
     * Custom ID for internal tracking.
     */
    @Column(name = "custom_id")
    private String customId;

    /**
     * Subscription start time.
     */
    @Column(name = "start_time")
    private OffsetDateTime startTime;

    /**
     * Next billing time.
     */
    @Column(name = "next_billing_time")
    private OffsetDateTime nextBillingTime;

    /**
     * Outstanding balance on the subscription.
     */
    @Column(name = "outstanding_balance", precision = 10, scale = 2)
    private BigDecimal outstandingBalance;

    /**
     * Number of billing cycles executed.
     */
    @Column(name = "cycles_completed")
    private Integer cyclesCompleted;

    /**
     * Number of failed payments.
     */
    @Column(name = "failed_payments_count")
    @Builder.Default
    private Integer failedPaymentsCount = 0;

    /**
     * Last payment time.
     */
    @Column(name = "last_payment_time")
    private OffsetDateTime lastPaymentTime;

    /**
     * Last payment amount.
     */
    @Column(name = "last_payment_amount", precision = 10, scale = 2)
    private BigDecimal lastPaymentAmount;

    /**
     * Whether auto-renewal is enabled.
     */
    @Column(name = "auto_renewal")
    @Builder.Default
    private Boolean autoRenewal = true;

    /**
     * Cancellation time if cancelled.
     */
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    /**
     * Status change reason (for suspensions/cancellations).
     */
    @Column(name = "status_change_reason", columnDefinition = "TEXT")
    private String statusChangeReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
