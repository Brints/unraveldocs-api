package com.extractor.unraveldocs.coupon.repository;

import com.extractor.unraveldocs.coupon.model.CouponUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, String> {

    List<CouponUsage> findByCouponId(String couponId);

    Page<CouponUsage> findByCouponId(String couponId, Pageable pageable);

    List<CouponUsage> findByUserId(String userId);

    Page<CouponUsage> findByUserId(String userId, Pageable pageable);

    /**
     * Count usage by coupon and user for per-user limit enforcement.
     */
    int countByCouponIdAndUserId(String couponId, String userId);

    /**
     * Check if user has used a specific coupon.
     */
    boolean existsByCouponIdAndUserId(String couponId, String userId);

    /**
     * Calculate total discount amount for a coupon.
     */
    @Query("SELECT COALESCE(SUM(u.discountAmount), 0) FROM CouponUsage u WHERE u.coupon.id = :couponId")
    BigDecimal sumDiscountAmountByCouponId(@Param("couponId") String couponId);

    /**
     * Calculate total original amount for a coupon.
     */
    @Query("SELECT COALESCE(SUM(u.originalAmount), 0) FROM CouponUsage u WHERE u.coupon.id = :couponId")
    BigDecimal sumOriginalAmountByCouponId(@Param("couponId") String couponId);

    /**
     * Count unique users for a coupon.
     */
    @Query("SELECT COUNT(DISTINCT u.user.id) FROM CouponUsage u WHERE u.coupon.id = :couponId")
    int countUniqueUsersByCouponId(@Param("couponId") String couponId);

    /**
     * Find usage within date range for analytics.
     */
    @Query("SELECT u FROM CouponUsage u WHERE u.coupon.id = :couponId " +
            "AND u.usedAt BETWEEN :startDate AND :endDate ORDER BY u.usedAt")
    List<CouponUsage> findByCouponIdAndDateRange(
            @Param("couponId") String couponId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    /**
     * Count usage by subscription plan for a coupon.
     */
    @Query("SELECT u.subscriptionPlan, COUNT(u) FROM CouponUsage u " +
            "WHERE u.coupon.id = :couponId GROUP BY u.subscriptionPlan")
    List<Object[]> countBySubscriptionPlanForCoupon(@Param("couponId") String couponId);

    /**
     * Find usages for daily analytics aggregation.
     */
    @Query("SELECT u FROM CouponUsage u WHERE u.usedAt BETWEEN :startOfDay AND :endOfDay")
    List<CouponUsage> findUsagesForDate(
            @Param("startOfDay") OffsetDateTime startOfDay,
            @Param("endOfDay") OffsetDateTime endOfDay);
}
