package com.extractor.unraveldocs.coupon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Entity
@Builder
@Table(name = "coupon_analytics", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "coupon_id", "analytics_date" })
})
@NoArgsConstructor
@AllArgsConstructor
public class CouponAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "analytics_date", nullable = false)
    private LocalDate analyticsDate;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "total_discount_amount", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal totalDiscountAmount = BigDecimal.ZERO;

    @Column(name = "total_original_amount", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal totalOriginalAmount = BigDecimal.ZERO;

    @Column(name = "total_final_amount", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal totalFinalAmount = BigDecimal.ZERO;

    @Column(name = "unique_users_count")
    private Integer uniqueUsersCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "by_subscription_plan", columnDefinition = "jsonb")
    private Map<String, Integer> bySubscriptionPlan;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "by_recipient_category", columnDefinition = "jsonb")
    private Map<String, Integer> byRecipientCategory;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;
}
