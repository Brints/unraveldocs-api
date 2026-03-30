package com.extractor.unraveldocs.metrics.dashboard.service.impl;

import com.extractor.unraveldocs.loginattempts.repository.LoginAttemptsRepository;
import com.extractor.unraveldocs.metrics.dashboard.dto.DashboardKpiStatsDto;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardMetricsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptsRepository loginAttemptsRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private DashboardMetricsCacheService dashboardMetricsCacheService;

    @Test
    void getDashboardKpiStatsData_returnsCorrectMetrics_whenDataExists() {
        // Arrange User KPIs
        when(userRepository.countByDeletedAtIsNull()).thenReturn(100L);
        when(userRepository.countByIsActiveTrueAndIsVerifiedTrueAndDeletedAtIsNull()).thenReturn(80L);
        when(userRepository.countByIsVerifiedTrueAndDeletedAtIsNull()).thenReturn(90L);
        when(userRepository.countByIsVerifiedFalseAndDeletedAtIsNull()).thenReturn(10L);
        when(loginAttemptsRepository.countByIsBlockedTrue()).thenReturn(5L);
        
        when(userRepository.countByCreatedAtAfterAndDeletedAtIsNull(any())).thenReturn(5L, 15L, 30L); 
        when(userRepository.countByLastLoginAfterAndDeletedAtIsNull(any())).thenReturn(20L, 50L, 85L);

        // Arrange Subscription KPIs
        when(userSubscriptionRepository.countByStatusIgnoreCase("ACTIVE")).thenReturn(60L);
        when(userSubscriptionRepository.countByStatusIgnoreCase("TRIAL")).thenReturn(10L);
        when(userSubscriptionRepository.countByStatusIgnoreCase("CANCELLED")).thenReturn(5L);
        when(userSubscriptionRepository.countByStatusIgnoreCase("EXPIRED")).thenReturn(25L);

        Object[] plan1 = {SubscriptionPlans.PRO_MONTHLY, 40L};
        Object[] plan2 = {"BUSINESS_MONTHLY", 20L};
        when(userSubscriptionRepository.countUsersByPlan()).thenReturn(Arrays.asList(plan1, plan2));

        Object[] status1 = {"ACTIVE", 60L};
        Object[] status2 = {"TRIAL", 10L};
        when(userSubscriptionRepository.countUsersByStatus()).thenReturn(Arrays.asList(status1, status2));

        // Arrange Storage KPIs
        when(userSubscriptionRepository.sumTotalStorageUsed()).thenReturn(500000L); // 500KB total over 100 users (avg 5000)

        // Act
        DashboardKpiStatsDto dto = dashboardMetricsCacheService.getDashboardKpiStatsData();

        // Assert
        assertNotNull(dto);
        assertEquals(100L, dto.getTotalUsers());
        assertEquals(80L, dto.getActiveUsers());
        assertEquals(90L, dto.getVerifiedUsers());
        assertEquals(10L, dto.getUnverifiedUsers());
        assertEquals(5L, dto.getBlockedUsers());

        assertEquals(5L, dto.getNewUsersToday());
        assertEquals(15L, dto.getNewUsersThisWeek());
        assertEquals(30L, dto.getNewUsersThisMonth());

        assertEquals(20L, dto.getDailyActiveUsers());
        assertEquals(50L, dto.getWeeklyActiveUsers());
        assertEquals(85L, dto.getMonthlyActiveUsers());

        assertEquals(60L, dto.getActiveSubscriptions());
        assertEquals(10L, dto.getTrialSubscriptions());
        
        assertEquals(40L, dto.getUsersByPlan().get("PRO_MONTHLY"));
        assertEquals(20L, dto.getUsersByPlan().get("BUSINESS_MONTHLY"));
        assertEquals(0L, dto.getUsersByPlan().get("FREE")); // ensure default keys exist
        
        assertEquals(60L, dto.getUsersByStatus().get("ACTIVE"));
        assertEquals(0L, dto.getUsersByStatus().get("EXPIRED"));

        assertEquals(500000L, dto.getTotalStorageUsed());
        assertEquals(5000.0, dto.getAverageStorageUsed(), 0.01);
    }

    @Test
    void getDashboardKpiStatsData_handlesNullAndEmptyData_correctly() {
        // Arrange returning zeros and nulls where appropriate
        when(userRepository.countByDeletedAtIsNull()).thenReturn(0L);
        when(userRepository.countByIsActiveTrueAndIsVerifiedTrueAndDeletedAtIsNull()).thenReturn(0L);
        when(userRepository.countByIsVerifiedTrueAndDeletedAtIsNull()).thenReturn(0L);
        when(userRepository.countByIsVerifiedFalseAndDeletedAtIsNull()).thenReturn(0L);
        when(loginAttemptsRepository.countByIsBlockedTrue()).thenReturn(0L);
        when(userRepository.countByCreatedAtAfterAndDeletedAtIsNull(any())).thenReturn(0L);
        when(userRepository.countByLastLoginAfterAndDeletedAtIsNull(any())).thenReturn(0L);

        when(userSubscriptionRepository.countByStatusIgnoreCase(anyString())).thenReturn(0L);
        when(userSubscriptionRepository.countUsersByPlan()).thenReturn(Collections.emptyList());
        when(userSubscriptionRepository.countUsersByStatus()).thenReturn(Collections.emptyList());
        
        // Null storage
        when(userSubscriptionRepository.sumTotalStorageUsed()).thenReturn(null);

        // Act
        DashboardKpiStatsDto dto = dashboardMetricsCacheService.getDashboardKpiStatsData();

        // Assert
        assertNotNull(dto);
        assertEquals(0L, dto.getTotalUsers());
        assertEquals(0L, dto.getTotalStorageUsed());
        assertEquals(0.0, dto.getAverageStorageUsed(), 0.01);
        
        // Default statuses and plans should still exist with 0
        assertEquals(0L, dto.getUsersByPlan().get("FREE"));
        assertEquals(0L, dto.getUsersByStatus().get("ACTIVE"));
    }
}
