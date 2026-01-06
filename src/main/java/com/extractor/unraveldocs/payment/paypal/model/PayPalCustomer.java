package com.extractor.unraveldocs.payment.paypal.model;

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
 * Entity for tracking PayPal customers (linked to PayPal payer accounts).
 */
@Data
@Entity
@Builder
@Table(name = "paypal_customers", indexes = {
        @Index(name = "idx_paypal_customer_payer_id", columnList = "payer_id"),
        @Index(name = "idx_paypal_customer_user_id", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    /**
     * PayPal Payer ID (unique identifier for the payer in PayPal).
     */
    @Column(name = "payer_id", unique = true)
    private String payerId;

    /**
     * Email address associated with the PayPal account.
     */
    @Column(name = "email")
    private String email;

    /**
     * First name from PayPal account.
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * Last name from PayPal account.
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * Phone number if provided by PayPal.
     */
    @Column(name = "phone")
    private String phone;

    /**
     * Country code from PayPal account.
     */
    @Column(name = "country_code")
    private String countryCode;

    /**
     * PayPal vault ID for stored payment methods.
     */
    @Column(name = "vault_id")
    private String vaultId;

    /**
     * Additional metadata in JSON format.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
