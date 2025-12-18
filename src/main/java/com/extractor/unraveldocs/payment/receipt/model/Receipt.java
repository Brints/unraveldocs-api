package com.extractor.unraveldocs.payment.receipt.model;

import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
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
 * Entity for tracking payment receipts
 */
@Data
@Entity
@Builder
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipt_number", columnList = "receipt_number", unique = true),
        @Index(name = "idx_receipt_user_id", columnList = "user_id"),
        @Index(name = "idx_receipt_external_payment", columnList = "external_payment_id, payment_provider"),
        @Index(name = "idx_receipt_created_at", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "receipt_number", nullable = false, unique = true)
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false)
    private PaymentProvider paymentProvider;

    @Column(name = "external_payment_id", nullable = false)
    private String externalPaymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_method_details")
    private String paymentMethodDetails;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "receipt_url", columnDefinition = "TEXT")
    private String receiptUrl;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @Column(name = "email_sent_at")
    private OffsetDateTime emailSentAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
