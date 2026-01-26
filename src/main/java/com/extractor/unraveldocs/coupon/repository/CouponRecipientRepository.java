package com.extractor.unraveldocs.coupon.repository;

import com.extractor.unraveldocs.coupon.model.CouponRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRecipientRepository extends JpaRepository<CouponRecipient, String> {

    List<CouponRecipient> findByCouponId(String couponId);

    Optional<CouponRecipient> findByCouponIdAndUserId(String couponId, String userId);

    boolean existsByCouponIdAndUserId(String couponId, String userId);

    List<CouponRecipient> findByUserId(String userId);

    /**
     * Find recipients who haven't been notified about a coupon.
     */
    @Query("SELECT r FROM CouponRecipient r WHERE r.coupon.id = :couponId AND r.notifiedAt IS NULL")
    List<CouponRecipient> findUnnotifiedByCouponId(@Param("couponId") String couponId);

    /**
     * Find recipients who haven't been notified about expiry.
     */
    @Query("SELECT r FROM CouponRecipient r WHERE r.coupon.id = :couponId AND r.expiryNotifiedAt IS NULL")
    List<CouponRecipient> findExpiryUnnotifiedByCouponId(@Param("couponId") String couponId);

    /**
     * Find all coupons available for a specific user (via recipient assignments).
     */
    @Query("SELECT r FROM CouponRecipient r WHERE r.user.id = :userId " +
            "AND r.coupon.isActive = true " +
            "AND r.coupon.validFrom <= CURRENT_TIMESTAMP " +
            "AND r.coupon.validUntil > CURRENT_TIMESTAMP")
    List<CouponRecipient> findActiveRecipientAssignmentsForUser(@Param("userId") String userId);

    /**
     * Count recipients for a coupon.
     */
    int countByCouponId(String couponId);

    /**
     * Delete all recipients for a coupon.
     */
    void deleteByCouponId(String couponId);
}
