package com.extractor.unraveldocs.payment.paystack.model;

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
 * Entity for tracking Paystack payment transactions
 */
@Data
@Entity
@Builder
@Table(name = "paystack_payments", indexes = {
        @Index(name = "idx_paystack_reference", columnList = "reference"),
        @Index(name = "idx_paystack_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_paystack_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_paystack_payment_status", columnList = "status"),
        @Index(name = "idx_paystack_payment_created_at", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class PaystackPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paystack_customer_id", referencedColumnName = "id")
    private PaystackCustomer paystackCustomer;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "access_code")
    private String accessCode;

    @Column(name = "authorization_url", columnDefinition = "TEXT")
    private String authorizationUrl;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "subscription_code")
    private String subscriptionCode;

    @Column(name = "plan_code")
    private String planCode;

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
    private BigDecimal amountRefunded;

    @Column(name = "fees", precision = 10, scale = 2)
    private BigDecimal fees;

    private String channel;

    @Column(name = "gateway_response")
    private String gatewayResponse;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}

