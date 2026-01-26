package com.extractor.unraveldocs.coupon.model;

import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Builder
@Table(name = "coupon_recipients", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "coupon_id", "user_id" })
})
@NoArgsConstructor
@AllArgsConstructor
public class CouponRecipient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Column(name = "expiry_notified_at")
    private OffsetDateTime expiryNotifiedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    /**
     * Marks the recipient as notified about the coupon availability.
     */
    public void markAsNotified() {
        this.notifiedAt = OffsetDateTime.now();
    }

    /**
     * Marks the recipient as notified about the coupon expiry.
     */
    public void markAsExpiryNotified() {
        this.expiryNotifiedAt = OffsetDateTime.now();
    }

    /**
     * Checks if the recipient has been notified about the coupon.
     */
    public boolean hasBeenNotified() {
        return notifiedAt != null;
    }
}
