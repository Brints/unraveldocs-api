package com.extractor.unraveldocs.metrics.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiStatsDto {
    // --- User KPIs ---
    private long totalUsers;
    private long activeUsers;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
    private long verifiedUsers;
    private long unverifiedUsers;
    private long blockedUsers;
    private long dailyActiveUsers;
    private long weeklyActiveUsers;
    private long monthlyActiveUsers;

    // --- Subscription KPIs ---
    private Map<String, Long> usersByPlan;
    private Map<String, Long> usersByStatus;
    private long activeSubscriptions;
    private long trialSubscriptions;
    private long cancelledSubscriptions;
    private long expiredSubscriptions;

    // --- Document/Storage KPIs ---
    private long totalStorageUsed;
    private double averageStorageUsed;
}
