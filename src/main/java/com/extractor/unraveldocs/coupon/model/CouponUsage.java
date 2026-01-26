package com.extractor.unraveldocs.coupon.model;

import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Entity
@Builder
@Table(name = "coupon_usage")
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_amount", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal finalAmount;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "subscription_plan", length = 100)
    private String subscriptionPlan;

    @Column(name = "used_at", nullable = false)
    private OffsetDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        if (usedAt == null) {
            usedAt = OffsetDateTime.now();
        }
    }
}
