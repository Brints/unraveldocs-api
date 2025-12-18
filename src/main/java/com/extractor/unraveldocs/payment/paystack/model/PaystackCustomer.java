package com.extractor.unraveldocs.payment.paystack.model;

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
 * Entity representing a Paystack customer
 */
@Data
@Entity
@Builder
@Table(name = "paystack_customers", indexes = {
        @Index(name = "idx_paystack_customer_code", columnList = "customer_code"),
        @Index(name = "idx_paystack_customer_user_id", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class PaystackCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(name = "customer_code", unique = true)
    private String customerCode;

    @Column(name = "paystack_customer_id")
    private Long paystackCustomerId;

    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String phone;

    @Column(name = "risk_action")
    private String riskAction;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}

