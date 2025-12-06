package com.extractor.unraveldocs.payment.stripe.model;

import com.extractor.unraveldocs.payment.stripe.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.stripe.enums.PaymentType;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity for tracking all payment transactions
 */
@Data
@Entity
@Table(name = "stripe_payments", indexes = {
        @Index(name = "idx_payment_intent_id", columnList = "payment_intent_id"),
        @Index(name = "idx_subscription_id", columnList = "subscription_id"),
        @Index(name = "idx_user_id_payment", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class StripePayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stripe_customer_id", referencedColumnName = "id")
    private StripeCustomer stripeCustomer;

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "subscription_id")
    private String subscriptionId;

    @Column(name = "invoice_id")
    private String invoiceId;

    @Column(name = "checkout_session_id")
    private String checkoutSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "payment_type")
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "amount_refunded", precision = 10, scale = 2)
    private BigDecimal amountRefunded = BigDecimal.ZERO;

    @Column(name = "payment_method_id")
    private String paymentMethodId;

    @Column(name = "receipt_url", columnDefinition = "TEXT")
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
