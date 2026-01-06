package com.extractor.unraveldocs.subscription.model;

import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "subscription_plans")
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private SubscriptionPlans name;

    @Column(nullable = false, name = "price", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private SubscriptionCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "billing_interval_unit")
    private BillingIntervalUnit billingIntervalUnit;

    @Column(nullable = false, name = "billing_interval_value")
    private Integer billingIntervalValue; // e.g., 1 for monthly, 12 for yearly

    @Column(nullable = false, name = "document_upload_limit")
    private Integer documentUploadLimit; // Maximum number of documents a user can upload

    @Column(nullable = false, name = "ocr_page_limit")
    private Integer ocrPageLimit;

    @Column(nullable = false, name = "is_active")
    private boolean isActive = true;

    @Column(name = "paystack_plan_code")
    private String paystackPlanCode;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "paypal_plan_code")
    private String paypalPlanCode;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Discount> discounts;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "storage_limit", nullable = false)
    private Long storageLimit; // Storage limit in bytes
}
