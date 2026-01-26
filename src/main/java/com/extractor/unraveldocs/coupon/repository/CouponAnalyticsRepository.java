package com.extractor.unraveldocs.coupon.repository;

import com.extractor.unraveldocs.coupon.model.CouponAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponAnalyticsRepository extends JpaRepository<CouponAnalytics, String> {

    List<CouponAnalytics> findByCouponId(String couponId);

    Optional<CouponAnalytics> findByCouponIdAndAnalyticsDate(String couponId, LocalDate analyticsDate);

    /**
     * Find analytics within date range for a coupon.
     */
    List<CouponAnalytics> findByCouponIdAndAnalyticsDateBetweenOrderByAnalyticsDateAsc(
            String couponId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Calculate aggregated statistics for a coupon within date range.
     */
    @Query("SELECT SUM(a.usageCount), SUM(a.totalDiscountAmount), SUM(a.totalOriginalAmount), " +
            "SUM(a.totalFinalAmount), SUM(a.uniqueUsersCount) " +
            "FROM CouponAnalytics a WHERE a.coupon.id = :couponId " +
            "AND a.analyticsDate BETWEEN :startDate AND :endDate")
    Object[] getAggregatedStatsByCouponId(
            @Param("couponId") String couponId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find all analytics for a specific date (for comparison).
     */
    List<CouponAnalytics> findByAnalyticsDate(LocalDate analyticsDate);

    /**
     * Delete analytics older than a certain date.
     */
    void deleteByAnalyticsDateBefore(LocalDate date);
}
