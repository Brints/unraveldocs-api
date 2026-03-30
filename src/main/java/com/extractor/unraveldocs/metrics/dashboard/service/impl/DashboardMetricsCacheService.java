package com.extractor.unraveldocs.metrics.dashboard.service.impl;

import com.extractor.unraveldocs.loginattempts.repository.LoginAttemptsRepository;
import com.extractor.unraveldocs.metrics.dashboard.dto.DashboardKpiStatsDto;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardMetricsCacheService {

    private final UserRepository userRepository;
    private final LoginAttemptsRepository loginAttemptsRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardKpiStatsData", key = "'stats'", unless = "#result == null")
    public DashboardKpiStatsDto getDashboardKpiStatsData() {
        log.info("Computing Dashboard KPI Stats...");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime startOfWeek = now.minusDays(7);
        OffsetDateTime startOfMonth = now.minusDays(30);

        // User KPIs
        long totalUsers = userRepository.countByDeletedAtIsNull();
        long activeUsers = userRepository.countByIsActiveTrueAndIsVerifiedTrueAndDeletedAtIsNull();
        long verifiedUsers = userRepository.countByIsVerifiedTrueAndDeletedAtIsNull();
        long unverifiedUsers = userRepository.countByIsVerifiedFalseAndDeletedAtIsNull();
        long blockedUsers = loginAttemptsRepository.countByIsBlockedTrue();

        long newUsersToday = userRepository.countByCreatedAtAfterAndDeletedAtIsNull(startOfToday);
        long newUsersThisWeek = userRepository.countByCreatedAtAfterAndDeletedAtIsNull(startOfWeek);
        long newUsersThisMonth = userRepository.countByCreatedAtAfterAndDeletedAtIsNull(startOfMonth);

        long dailyActiveUsers = userRepository.countByLastLoginAfterAndDeletedAtIsNull(startOfToday);
        long weeklyActiveUsers = userRepository.countByLastLoginAfterAndDeletedAtIsNull(startOfWeek);
        long monthlyActiveUsers = userRepository.countByLastLoginAfterAndDeletedAtIsNull(startOfMonth);

        // Subscription KPIs
        long activeSubscriptions = userSubscriptionRepository.countByStatusIgnoreCase("ACTIVE");
        long trialSubscriptions = userSubscriptionRepository.countByStatusIgnoreCase("TRIAL");
        long cancelledSubscriptions = userSubscriptionRepository.countByStatusIgnoreCase("CANCELLED");
        long expiredSubscriptions = userSubscriptionRepository.countByStatusIgnoreCase("EXPIRED");

        Map<String, Long> usersByPlan = fetchUsersByPlan();
        Map<String, Long> usersByStatus = fetchUsersByStatus();

        // Storage KPIs
        Long totalStorage = userSubscriptionRepository.sumTotalStorageUsed();
        long totalStorageUsed = totalStorage != null ? totalStorage : 0L;
        double averageStorageUsed = totalUsers > 0 ? (double) totalStorageUsed / totalUsers : 0.0;

        return DashboardKpiStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .verifiedUsers(verifiedUsers)
                .unverifiedUsers(unverifiedUsers)
                .blockedUsers(blockedUsers)
                .dailyActiveUsers(dailyActiveUsers)
                .weeklyActiveUsers(weeklyActiveUsers)
                .monthlyActiveUsers(monthlyActiveUsers)
                .usersByPlan(usersByPlan)
                .usersByStatus(usersByStatus)
                .activeSubscriptions(activeSubscriptions)
                .trialSubscriptions(trialSubscriptions)
                .cancelledSubscriptions(cancelledSubscriptions)
                .expiredSubscriptions(expiredSubscriptions)
                .totalStorageUsed(totalStorageUsed)
                .averageStorageUsed(averageStorageUsed)
                .build();
    }

    private Map<String, Long> fetchUsersByPlan() {
        Map<String, Long> planCountMap = new HashMap<>();
        // Fill defaults so clients always receive all known plans.
        for (SubscriptionPlans plan : SubscriptionPlans.values()) {
            planCountMap.put(plan.name(), 0L);
        }

        List<Object[]> queryResults = userSubscriptionRepository.countUsersByPlan();
        for (Object[] result : queryResults) {
            String planName = "UNKNOWN";
            if (result[0] instanceof SubscriptionPlans) {
                planName = ((SubscriptionPlans) result[0]).name();
            } else if (result[0] != null) {
                planName = result[0].toString();
            }
            Long count = (Long) result[1];
            planCountMap.put(planName, count);
        }
        return planCountMap;
    }

    private Map<String, Long> fetchUsersByStatus() {
        Map<String, Long> statusCountMap = new HashMap<>();
        statusCountMap.put("ACTIVE", 0L);
        statusCountMap.put("TRIAL", 0L);
        statusCountMap.put("CANCELLED", 0L);
        statusCountMap.put("EXPIRED", 0L);
        statusCountMap.put("PENDING", 0L);

        List<Object[]> queryResults = userSubscriptionRepository.countUsersByStatus();
        for (Object[] result : queryResults) {
            String statusName = result[0] != null ? result[0].toString() : "UNKNOWN";
            Long count = (Long) result[1];
            statusCountMap.put(statusName.toUpperCase(), count);
        }
        return statusCountMap;
    }
}


