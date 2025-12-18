package com.extractor.unraveldocs.payment.paystack.model;

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
 * Entity representing a Paystack subscription
 */
@Data
@Entity
@Builder
@Table(name = "paystack_subscriptions", indexes = {
        @Index(name = "idx_paystack_subscription_code", columnList = "subscription_code"),
        @Index(name = "idx_paystack_subscription_user_id", columnList = "user_id"),
        @Index(name = "idx_paystack_subscription_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
public class PaystackSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paystack_customer_id", referencedColumnName = "id")
    private PaystackCustomer paystackCustomer;

    @Column(name = "plan_code")
    private String planCode;

    @Column(name = "paystack_subscription_id")
    private Long paystackSubscriptionId;

    @Column(name = "subscription_code", unique = true, nullable = false)
    private String subscriptionCode;

    @Column(name = "email_token")
    private String emailToken;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(nullable = false)
    private String status;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "next_payment_date")
    private OffsetDateTime nextPaymentDate;

    @Column(name = "invoice_limit")
    private Integer invoiceLimit;

    @Column(name = "payments_count")
    private Integer paymentsCount;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}

