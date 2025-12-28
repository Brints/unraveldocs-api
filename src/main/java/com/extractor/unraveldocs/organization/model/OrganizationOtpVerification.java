package com.extractor.unraveldocs.organization.model;

import com.extractor.unraveldocs.organization.datamodel.OrganizationSubscriptionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "organization_otp_verifications", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "otp"),
        @Index(columnList = "expires_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationOtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "organization_description")
    private String organizationDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    private OrganizationSubscriptionType subscriptionType;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "payment_token")
    private String paymentToken; // Token from payment gateway for setting up subscription

    @Column(nullable = false)
    private String otp;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    // Helper methods
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isUsed && !isExpired();
    }

    public void markAsUsed() {
        this.isUsed = true;
    }
}
