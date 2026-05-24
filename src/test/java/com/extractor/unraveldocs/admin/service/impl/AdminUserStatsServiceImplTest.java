package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.UserStatsDto;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.loginattempts.repository.LoginAttemptsRepository;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserStatsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptsRepository loginAttemptsRepository;

    @InjectMocks
    private AdminUserStatsServiceImpl testClass;

    /** Helper to build List<Object[]> without varargs ambiguity. */
    private static List<Object[]> rows(Object[]... items) {
        List<Object[]> list = new ArrayList<>();
        Collections.addAll(list, items);
        return list;
    }

    @BeforeEach
    void setUp() {
        // Totals
        when(userRepository.countByDeletedAtIsNull()).thenReturn(100L);
        when(userRepository.countByIsActiveTrueAndDeletedAtIsNull()).thenReturn(80L);
        when(userRepository.countByIsActiveFalseAndDeletedAtIsNull()).thenReturn(20L);
        when(userRepository.countByIsVerifiedTrueAndDeletedAtIsNull()).thenReturn(75L);
        when(userRepository.countByIsVerifiedFalseAndDeletedAtIsNull()).thenReturn(25L);
        when(loginAttemptsRepository.countByIsBlockedTrue()).thenReturn(3L);
        when(userRepository.countByDeletedAtIsNotNull()).thenReturn(5L);
        when(userRepository.countByMarketingOptInTrueAndDeletedAtIsNull()).thenReturn(40L);

        // Activity windows
        when(userRepository.countByCreatedAtAfterAndDeletedAtIsNull(any(OffsetDateTime.class)))
                .thenReturn(10L);
        when(userRepository.countByLastLoginAfterAndDeletedAtIsNull(any(OffsetDateTime.class)))
                .thenReturn(50L);

        // Breakdowns
        when(userRepository.countByRoleGrouped()).thenReturn(rows(
                new Object[]{Role.USER, 90L},
                new Object[]{Role.ADMIN, 10L}
        ));
        when(userRepository.countByCountryGrouped()).thenReturn(rows(
                new Object[]{"US", 40L},
                new Object[]{"NG", 30L},
                new Object[]{"UK", 20L}
        ));
        when(userRepository.countByProfessionGrouped()).thenReturn(rows(
                new Object[]{"Developer", 50L},
                new Object[]{"Designer", 30L}
        ));
        when(userRepository.countByOrganizationGrouped()).thenReturn(rows(
                new Object[]{"Acme Corp", 25L}
        ));

        // Growth series (empty — the method still fills zero-days)
        when(userRepository.countNewUsersPerDaySince(any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void getUserStatistics_returnsTotals() {
        UserStatsDto stats = testClass.getUserStatistics();

        assertEquals(100L, stats.getTotalUsers());
        assertEquals(80L, stats.getActiveUsers());
        assertEquals(20L, stats.getInactiveUsers());
        assertEquals(75L, stats.getVerifiedUsers());
        assertEquals(25L, stats.getUnverifiedUsers());
        assertEquals(3L, stats.getBlockedUsers());
        assertEquals(5L, stats.getSoftDeletedUsers());
    }

    @Test
    void getUserStatistics_returnsActivityWindows() {
        UserStatsDto stats = testClass.getUserStatistics();

        assertEquals(10L, stats.getNewUsersToday());
        assertEquals(10L, stats.getNewUsersThisWeek());
        assertEquals(10L, stats.getNewUsersThisMonth());
        assertEquals(50L, stats.getDailyActiveUsers());
        assertEquals(50L, stats.getWeeklyActiveUsers());
        assertEquals(50L, stats.getMonthlyActiveUsers());
    }

    @Test
    void getUserStatistics_computesRates() {
        UserStatsDto stats = testClass.getUserStatistics();

        assertEquals(75.0, stats.getVerificationRate());
        assertEquals(40.0, stats.getMarketingOptInRate());
    }

    @Test
    void getUserStatistics_returnsBreakdowns() {
        UserStatsDto stats = testClass.getUserStatistics();

        assertEquals(2, stats.getUsersByRole().size());
        assertEquals(90L, stats.getUsersByRole().get("user"));
        assertEquals(10L, stats.getUsersByRole().get("admin"));

        assertEquals(3, stats.getUsersByCountry().size());
        assertEquals(40L, stats.getUsersByCountry().get("US"));

        assertEquals(2, stats.getUsersByProfession().size());
        assertEquals(50L, stats.getUsersByProfession().get("Developer"));

        assertEquals(1, stats.getUsersByOrganization().size());
        assertEquals(25L, stats.getUsersByOrganization().get("Acme Corp"));
    }

    @Test
    void getUserStatistics_returnsGrowthSeries() {
        UserStatsDto stats = testClass.getUserStatistics();

        assertNotNull(stats.getUserGrowthSeries());
        assertTrue(stats.getUserGrowthSeries().size() >= 30);
        stats.getUserGrowthSeries().values().forEach(count -> assertEquals(0L, count));
    }
}
