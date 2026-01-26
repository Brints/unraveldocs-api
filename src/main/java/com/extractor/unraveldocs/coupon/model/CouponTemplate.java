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
@Table(name = "coupon_templates")
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "discount_percentage", nullable = false, columnDefinition = "DECIMAL(5,2)")
    private BigDecimal discountPercentage;

    @Column(name = "min_purchase_amount", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal minPurchaseAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_category", nullable = false, length = 50)
    private RecipientCategory recipientCategory;

    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Column(name = "max_usage_per_user", nullable = false)
    private Integer maxUsagePerUser = 1;

    @Column(name = "validity_days", nullable = false)
    private Integer validityDays;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "template")
    @Builder.Default
    private List<Coupon> coupons = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
