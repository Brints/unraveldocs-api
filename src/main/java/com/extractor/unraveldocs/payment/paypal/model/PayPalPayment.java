package com.extractor.unraveldocs.payment.paypal.model;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
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
 * Entity for tracking PayPal payment transactions.
 */
@Data
@Entity
@Builder
@Table(name = "paypal_payments", indexes = {
        @Index(name = "idx_paypal_order_id", columnList = "order_id"),
        @Index(name = "idx_paypal_capture_id", columnList = "capture_id"),
        @Index(name = "idx_paypal_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_paypal_payment_status", columnList = "status"),
        @Index(name = "idx_paypal_payment_created_at", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class PayPalPayment {

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
     * PayPal Order ID.
     */
    @Column(name = "order_id", unique = true)
    private String orderId;

    /**
     * PayPal Capture ID (for captured payments).
     */
    @Column(name = "capture_id")
    private String captureId;

    /**
     * PayPal Authorization ID (for authorized payments).
     */
    @Column(name = "authorization_id")
    private String authorizationId;

    /**
     * Associated subscription ID if this is a subscription payment.
     */
    @Column(name = "subscription_id")
    private String subscriptionId;

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
    @Builder.Default
    private BigDecimal amountRefunded = BigDecimal.ZERO;

    /**
     * PayPal transaction fee.
     */
    @Column(name = "paypal_fee", precision = 10, scale = 2)
    private BigDecimal paypalFee;

    /**
     * Net amount after fees.
     */
    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    /**
     * Payment intent: CAPTURE or AUTHORIZE.
     */
    @Column(name = "intent")
    private String intent;

    /**
     * Payer ID from PayPal.
     */
    @Column(name = "payer_id")
    private String payerId;

    /**
     * Payer email from PayPal.
     */
    @Column(name = "payer_email")
    private String payerEmail;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
