package com.extractor.unraveldocs.subscription.model;

import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "user_subscriptions")
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", referencedColumnName = "id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "payment_gateway_subscription_id")
    private String paymentGatewaySubscriptionId;

    @Column(nullable = false)
    private String status;

    @Column(name = "current_period_start")
    private OffsetDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = false;

    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    @Column(nullable = false, name = "has_used_trial")
    private boolean hasUsedTrial = false;

    @Column(name = "storage_used", nullable = false)
    private Long storageUsed = 0L; // Current storage usage in bytes

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}