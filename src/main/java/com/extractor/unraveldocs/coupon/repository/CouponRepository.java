package com.extractor.unraveldocs.coupon.repository;

import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.coupon.model.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, String> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    Page<Coupon> findByIsActive(boolean isActive, Pageable pageable);

    Page<Coupon> findByRecipientCategory(RecipientCategory recipientCategory, Pageable pageable);

    Page<Coupon> findByIsActiveAndRecipientCategory(boolean isActive, RecipientCategory recipientCategory,
            Pageable pageable);

    List<Coupon> findByCreatedById(String userId);

    List<Coupon> findByTemplateId(String templateId);

    /**
     * Find valid coupons by code (active and within date range).
     */
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.isActive = true " +
            "AND c.validFrom <= :now AND c.validUntil > :now")
    Optional<Coupon> findValidCouponByCode(@Param("code") String code, @Param("now") OffsetDateTime now);

    /**
     * Find coupons expiring within a certain number of days that haven't been
     * notified.
     */
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
            "AND c.expiryNotificationSent = false " +
            "AND c.validUntil BETWEEN :now AND :expiryDate")
    List<Coupon> findCouponsExpiringBetween(
            @Param("now") OffsetDateTime now,
            @Param("expiryDate") OffsetDateTime expiryDate);

    /**
     * Find active coupons for a specific recipient category.
     */
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
            "AND c.validFrom <= :now AND c.validUntil > :now " +
            "AND c.recipientCategory = :category")
    List<Coupon> findActiveByRecipientCategory(
            @Param("category") RecipientCategory category,
            @Param("now") OffsetDateTime now);

    /**
     * Find all currently valid coupons (for Redis cache warming).
     */
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
            "AND c.validFrom <= :now AND c.validUntil > :now " +
            "ORDER BY c.createdAt DESC")
    List<Coupon> findAllValidCoupons(@Param("now") OffsetDateTime now, Pageable pageable);

    /**
     * Count coupons by template.
     */
    int countByTemplateId(String templateId);
}
