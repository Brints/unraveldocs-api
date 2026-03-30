package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.SubscriptionStatsDto;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSubscriptionStatsServiceImplTest {

    @Mock
    private UserSubscriptionRepository userRepository;

    @InjectMocks
    private AdminSubscriptionStatsServiceImpl testClass;

    private static List<Object[]> rows(Object[]... items) {
        List<Object[]> list = new ArrayList<>();
        Collections.addAll(list, items);
        return list;
    }

    @BeforeEach
    void setUp() {
        when(userRepository.count()).thenReturn(1500L);

        when(userRepository.countUsersByPlan()).thenReturn(rows(
                new Object[]{"FREE", 800L},
                new Object[]{"STARTER_MONTHLY", 200L},
                new Object[]{"PRO_YEARLY", 100L}
        ));

        when(userRepository.countUsersByStatus()).thenReturn(rows(
                new Object[]{"ACTIVE", 700L},
                new Object[]{"TRIAL", 200L},
                new Object[]{"CANCELLED", 100L},
                new Object[]{"EXPIRED", 500L}
        ));

        when(userRepository.countBySubscriptionSourceGrouped()).thenReturn(rows(
                new Object[]{"INDIVIDUAL", 1200L},
                new Object[]{"TEAM", 300L}
        ));

        when(userRepository.countActivePaidSubscriptions()).thenReturn(600L); // Assuming 700 active - 100 active free

        when(userRepository.countByHasUsedTrialTrueAndStatusIgnoreCase("ACTIVE")).thenReturn(90L);
        when(userRepository.countByHasUsedTrialTrueAndStatusIgnoreCase("EXPIRED")).thenReturn(60L);
        when(userRepository.countByHasUsedTrialTrueAndStatusIgnoreCase("CANCELLED")).thenReturn(50L);

        when(userRepository.countByStatusInIgnoreCase(List.of("EXPIRED", "CANCELLED"))).thenReturn(600L);

        // MRR
        when(userRepository.sumPricesForActivePaidSubscriptionsGroupedByInterval()).thenReturn(rows(
                // 100 monthly users at $10/mo = $1000 MRR
                new Object[]{1, BillingIntervalUnit.MONTH, BigDecimal.valueOf(1000.00)},
                // 50 yearly users at $120/yr (interval 1) = $500 MRR (120*50 / 12)
                new Object[]{1, BillingIntervalUnit.YEAR, BigDecimal.valueOf(6000.00)}
        ));

        // Quota warnings
        when(userRepository.countUsersNearStorageLimit()).thenReturn(45L);
        when(userRepository.countUsersNearOcrLimit()).thenReturn(23L);
        when(userRepository.countUsersNearDocLimit()).thenReturn(67L);
        when(userRepository.countUsersNearAiLimit()).thenReturn(12L);
    }

    @Test
    void getSubscriptionStatistics_aggregatesCorrectly() {
        SubscriptionStatsDto stats = testClass.getSubscriptionStatistics();

        assertNotNull(stats);
        assertEquals(1500L, stats.getTotalSubscriptions());
        
        // Breakdowns
        assertEquals(3, stats.getByPlan().size());
        assertEquals(200L, stats.getByPlan().get("STARTER_MONTHLY"));
        
        assertEquals(4, stats.getByStatus().size());
        assertEquals(700L, stats.getByStatus().get("ACTIVE"));
        
        assertEquals(2, stats.getBySource().size());
        assertEquals(1200L, stats.getBySource().get("INDIVIDUAL"));

        // Rates
        // total trials = 90 + 60 + 50 = 200
        // active = 90
        // conversion rate = 90 / 200 = 45%
        assertEquals(45.0, stats.getTrialConversionRate());

        // total users = 1500, cancelled=100, expired=500 -> churned = 600
        // churn rate = 600 / 1500 = 40%
        assertEquals(40.0, stats.getChurnRate());

        // MRR = 1000 (monthly) + 6000 / 12 (yearly) = 1000 + 500 = 1500
        assertEquals(BigDecimal.valueOf(1500.00).setScale(2), stats.getMrr());

        // ARPU = 1500 MRR / 600 activePaidUsers = 2.50
        assertEquals(BigDecimal.valueOf(2.50).setScale(2), stats.getAverageRevenuePerUser());

        // Warnings
        assertEquals(45L, stats.getUsersNearingQuotaLimits().getStorage());
        assertEquals(23L, stats.getUsersNearingQuotaLimits().getOcrPages());
        assertEquals(67L, stats.getUsersNearingQuotaLimits().getDocumentUploads());
        assertEquals(12L, stats.getUsersNearingQuotaLimits().getAiOperations());
    }
}
