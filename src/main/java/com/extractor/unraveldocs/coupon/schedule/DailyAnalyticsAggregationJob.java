package com.extractor.unraveldocs.coupon.schedule;

import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.model.CouponAnalytics;
import com.extractor.unraveldocs.coupon.model.CouponUsage;
import com.extractor.unraveldocs.coupon.repository.CouponAnalyticsRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.repository.CouponUsageRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scheduled job to aggregate daily coupon analytics.
 * Runs daily at 1 AM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAnalyticsAggregationJob {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponAnalyticsRepository couponAnalyticsRepository;
    private final SanitizeLogging sanitizer;

    /**
     * Aggregates usage data from the previous day into the coupon_analytics table.
     */
    @Scheduled(cron = "${coupon.analytics.aggregation.cron:0 0 1 * * *}")
    @Transactional
    public void aggregateDailyAnalytics() {
        log.info("Starting daily analytics aggregation job");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        OffsetDateTime startOfDay = yesterday.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = yesterday.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        // Get all usages from yesterday
        List<CouponUsage> usages = couponUsageRepository.findUsagesForDate(startOfDay, endOfDay);
        log.info("Found {} usages for date: {}", sanitizer.sanitizeLoggingInteger(usages.size()), sanitizer.sanitizeLoggingObject(yesterday));

        // Group by coupon
        Map<Coupon, List<CouponUsage>> usagesByCoupon = usages.stream()
                .collect(Collectors.groupingBy(CouponUsage::getCoupon));

        for (Map.Entry<Coupon, List<CouponUsage>> entry : usagesByCoupon.entrySet()) {
            aggregateForCoupon(entry.getKey(), entry.getValue(), yesterday);
        }

        log.info("Daily analytics aggregation job completed");
    }

    private void aggregateForCoupon(Coupon coupon, List<CouponUsage> usages, LocalDate date) {
        // Calculate stats
        int usageCount = usages.size();
        int uniqueUsers = (int) usages.stream().map(u -> u.getUser().getId()).distinct().count();

        BigDecimal totalDiscount = usages.stream()
                .map(CouponUsage::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOriginal = usages.stream()
                .map(CouponUsage::getOriginalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFinal = usages.stream()
                .map(CouponUsage::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by subscription plan
        Map<String, Integer> byPlan = usages.stream()
                .filter(u -> u.getSubscriptionPlan() != null)
                .collect(Collectors.groupingBy(
                        CouponUsage::getSubscriptionPlan,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        // Check if analytics record already exists
        CouponAnalytics analytics = couponAnalyticsRepository
                .findByCouponIdAndAnalyticsDate(coupon.getId(), date)
                .orElseGet(() -> CouponAnalytics.builder()
                        .coupon(coupon)
                        .analyticsDate(date)
                        .build());

        // Update stats
        analytics.setUsageCount(usageCount);
        analytics.setUniqueUsersCount(uniqueUsers);
        analytics.setTotalDiscountAmount(totalDiscount);
        analytics.setTotalOriginalAmount(totalOriginal);
        analytics.setTotalFinalAmount(totalFinal);
        analytics.setBySubscriptionPlan(byPlan);

        couponAnalyticsRepository.save(analytics);
        log.debug("Saved analytics for coupon {} on {}", sanitizer.sanitizeLogging(coupon.getCode()), sanitizer.sanitizeLoggingObject(date));
    }
}
