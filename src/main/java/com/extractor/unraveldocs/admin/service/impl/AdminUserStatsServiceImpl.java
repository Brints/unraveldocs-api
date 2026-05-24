package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.UserStatsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminUserStatsService;
import com.extractor.unraveldocs.loginattempts.repository.LoginAttemptsRepository;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserStatsServiceImpl implements AdminUserStatsService {

    private final UserRepository userRepository;
    private final LoginAttemptsRepository loginAttemptsRepository;

    @Override
    @Transactional(readOnly = true)
    public UserStatsDto getUserStatistics() {
        log.info("Aggregating user statistics for admin dashboard");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime startOfWeek = now.minusDays(7);
        OffsetDateTime startOfMonth = now.minusDays(30);

        // --- Totals ---
        long totalUsers = userRepository.countByDeletedAtIsNull();
        long activeUsers = userRepository.countByIsActiveTrueAndDeletedAtIsNull();
        long inactiveUsers = userRepository.countByIsActiveFalseAndDeletedAtIsNull();
        long verifiedUsers = userRepository.countByIsVerifiedTrueAndDeletedAtIsNull();
        long unverifiedUsers = userRepository.countByIsVerifiedFalseAndDeletedAtIsNull();
        long blockedUsers = loginAttemptsRepository.countByIsBlockedTrue();
        long softDeletedUsers = userRepository.countByDeletedAtIsNotNull();

        // --- Activity windows ---
        long newUsersToday = userRepository.countByCreatedAtAfterAndDeletedAtIsNull(startOfToday);
        long newUsersThisWeek = userRepository.countByCreatedAtAfterAndDeletedAtIsNull(startOfWeek);
        long newUsersThisMonth = userRepository.countByCreatedAtAfterAndDeletedAtIsNull(startOfMonth);
        long dau = userRepository.countByLastLoginAfterAndDeletedAtIsNull(startOfToday);
        long wau = userRepository.countByLastLoginAfterAndDeletedAtIsNull(startOfWeek);
        long mau = userRepository.countByLastLoginAfterAndDeletedAtIsNull(startOfMonth);

        // --- Ratios ---
        double verificationRate = totalUsers > 0 ? (double) verifiedUsers / totalUsers * 100 : 0;
        long marketingOptIn = userRepository.countByMarketingOptInTrueAndDeletedAtIsNull();
        double marketingOptInRate = totalUsers > 0 ? (double) marketingOptIn / totalUsers * 100 : 0;

        // --- Breakdowns ---
        Map<String, Long> usersByRole = toMap(userRepository.countByRoleGrouped());
        Map<String, Long> usersByCountry = toMap(userRepository.countByCountryGrouped());
        Map<String, Long> usersByProfession = toMap(userRepository.countByProfessionGrouped());
        Map<String, Long> usersByOrganization = toMap(userRepository.countByOrganizationGrouped());

        // --- Growth series (last 30 days) ---
        Map<String, Long> growthSeries = buildGrowthSeries(startOfMonth);

        return UserStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .verifiedUsers(verifiedUsers)
                .unverifiedUsers(unverifiedUsers)
                .blockedUsers(blockedUsers)
                .softDeletedUsers(softDeletedUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .dailyActiveUsers(dau)
                .weeklyActiveUsers(wau)
                .monthlyActiveUsers(mau)
                .verificationRate(Math.round(verificationRate * 100.0) / 100.0)
                .marketingOptInRate(Math.round(marketingOptInRate * 100.0) / 100.0)
                .usersByRole(usersByRole)
                .usersByCountry(usersByCountry)
                .usersByProfession(usersByProfession)
                .usersByOrganization(usersByOrganization)
                .userGrowthSeries(growthSeries)
                .build();
    }

    /**
     * Converts a list of [key, count] tuples returned from GROUP BY queries
     * into a LinkedHashMap preserving query order.
     */
    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "Unknown";
            Long count = ((Number) row[1]).longValue();
            map.put(key, count);
        }
        return map;
    }

    /**
     * Builds a complete 30-day series filling in zeros for days with no registrations.
     */
    private Map<String, Long> buildGrowthSeries(OffsetDateTime since) {
        List<Object[]> rawSeries = userRepository.countNewUsersPerDaySince(since);

        Map<String, Long> dataFromDb = new LinkedHashMap<>();
        for (Object[] row : rawSeries) {
            String dateStr = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            dataFromDb.put(dateStr, count);
        }

        // Fill in every day from since to today
        Map<String, Long> completeSeries = new LinkedHashMap<>();
        LocalDate start = since.toLocalDate();
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String key = date.toString(); // YYYY-MM-DD
            completeSeries.put(key, dataFromDb.getOrDefault(key, 0L));
        }

        return completeSeries;
    }
}
