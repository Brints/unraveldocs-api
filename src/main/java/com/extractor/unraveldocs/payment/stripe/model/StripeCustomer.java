package com.extractor.unraveldocs.payment.stripe.model;

import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Entity representing a Stripe customer linked to a user
 */
@Data
@Entity
@Table(name = "stripe_customers", indexes = {
        @Index(name = "idx_stripe_customer_id", columnList = "stripe_customer_id"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class StripeCustomer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(name = "stripe_customer_id", nullable = false, unique = true)
    private String stripeCustomerId;

    @Column(nullable = false)
    private String email;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "default_payment_method_id")
    private String defaultPaymentMethodId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
