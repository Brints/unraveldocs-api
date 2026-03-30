package com.extractor.unraveldocs.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Aggregated user statistics DTO for the admin dashboard.
 * Covers growth, demographics, role distribution, and status breakdowns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {

    // --- Totals ---
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long verifiedUsers;
    private long unverifiedUsers;
    private long blockedUsers;
    private long softDeletedUsers;

    // --- Activity windows ---
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
    private long dailyActiveUsers;   // logged in today
    private long weeklyActiveUsers;  // logged in last 7 days
    private long monthlyActiveUsers; // logged in last 30 days

    // --- Ratios (pre-computed for the frontend) ---
    private double verificationRate;   // verifiedUsers / totalUsers * 100
    private double marketingOptInRate; // marketingOptIn / totalUsers * 100

    // --- Breakdowns (chart-ready) ---
    private Map<String, Long> usersByRole;
    private Map<String, Long> usersByCountry;
    private Map<String, Long> usersByProfession;
    private Map<String, Long> usersByOrganization;

    // --- Growth series (last 30 data points, keyed by date string YYYY-MM-DD) ---
    private Map<String, Long> userGrowthSeries;
}
