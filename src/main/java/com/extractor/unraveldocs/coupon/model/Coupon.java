package com.extractor.unraveldocs.coupon.model;

import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
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
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@Table(name = "coupons")
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "is_custom_code", nullable = false)
    private boolean isCustomCode = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_category", nullable = false, length = 50)
    private RecipientCategory recipientCategory = RecipientCategory.ALL_PAID_USERS;

    @Column(name = "discount_percentage", nullable = false, columnDefinition = "DECIMAL(5,2)")
    private BigDecimal discountPercentage;

    @Column(name = "min_purchase_amount", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal minPurchaseAmount;

    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Column(name = "max_usage_per_user", nullable = false)
    private Integer maxUsagePerUser = 1;

    @Column(name = "current_usage_count", nullable = false)
    private Integer currentUsageCount = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil;

    @Column(name = "expiry_notification_sent", nullable = false)
    private boolean expiryNotificationSent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private CouponTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CouponUsage> usages = new ArrayList<>();

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CouponRecipient> recipients = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Version field for optimistic locking.
     * Prevents race conditions when multiple requests try to increment usage count
     * simultaneously.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Checks if the coupon is currently valid based on date range.
     */
    public boolean isCurrentlyValid() {
        OffsetDateTime now = OffsetDateTime.now();
        return isActive && now.isAfter(validFrom) && now.isBefore(validUntil);
    }

    /**
     * Checks if total usage limit has been reached.
     */
    public boolean hasReachedUsageLimit() {
        return maxUsageCount != null && currentUsageCount >= maxUsageCount;
    }

    /**
     * Increments the usage count.
     */
    public void incrementUsageCount() {
        this.currentUsageCount++;
    }
}
