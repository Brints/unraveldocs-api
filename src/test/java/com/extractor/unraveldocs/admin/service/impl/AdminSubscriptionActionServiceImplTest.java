package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.AdjustSubscriptionDto;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminSubscriptionActionServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private AdminSubscriptionActionServiceImpl testClass;

    private User sampleUser;
    private UserSubscription sampleSubscription;
    private SubscriptionPlan targetPlan;
    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(userId);

        SubscriptionPlan oldPlan = new SubscriptionPlan();
        oldPlan.setName(SubscriptionPlans.FREE);

        sampleSubscription = new UserSubscription();
        sampleSubscription.setId("sub-123");
        sampleSubscription.setPlan(oldPlan);
        sampleSubscription.setOcrPagesUsed(50);
        sampleSubscription.setMonthlyDocumentsUploaded(10);
        sampleSubscription.setAiOperationsUsed(5);

        targetPlan = new SubscriptionPlan();
        targetPlan.setName(SubscriptionPlans.PRO_MONTHLY);
    }

    @Test
    void adjustUserSubscription_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userSubscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sampleSubscription));
        when(subscriptionPlanRepository.findByName(SubscriptionPlans.PRO_MONTHLY)).thenReturn(Optional.of(targetPlan));

        AdjustSubscriptionDto request = AdjustSubscriptionDto.builder()
                .plan(SubscriptionPlans.PRO_MONTHLY)
                .billingIntervalUnit(BillingIntervalUnit.MONTH)
                .billingIntervalValue(1)
                .autoRenew(true)
                .source("ADMIN_OVERRIDE")
                .build();

        UnravelDocsResponse<String> response = testClass.adjustUserSubscription(userId, request);

        assertEquals(SubscriptionPlans.PRO_MONTHLY, sampleSubscription.getPlan().getName());
        assertEquals(SubscriptionPlans.FREE, sampleSubscription.getPreviousPlan().getName());
        assertTrue(sampleSubscription.isAutoRenew());
        assertNotNull(sampleSubscription.getCurrentPeriodStart());
        assertNotNull(sampleSubscription.getCurrentPeriodEnd());
        verify(userSubscriptionRepository).save(sampleSubscription);
        assertNotNull(response);
    }

    @Test
    void resetUserQuotas_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userSubscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sampleSubscription));

        UnravelDocsResponse<String> response = testClass.resetUserQuotas(userId);

        assertEquals(0, sampleSubscription.getOcrPagesUsed());
        assertEquals(0, sampleSubscription.getMonthlyDocumentsUploaded());
        assertEquals(0, sampleSubscription.getAiOperationsUsed());
        assertNotNull(sampleSubscription.getQuotaResetDate());
        verify(userSubscriptionRepository).save(sampleSubscription);
        assertNotNull(response);
    }

    @Test
    void adjustUserSubscription_UserNotFound() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        AdjustSubscriptionDto request = AdjustSubscriptionDto.builder().build();
        assertThrows(NotFoundException.class, () -> testClass.adjustUserSubscription(userId, request));
    }

    @Test
    void adjustUserSubscription_SubscriptionNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userSubscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        AdjustSubscriptionDto request = AdjustSubscriptionDto.builder().build();
        assertThrows(NotFoundException.class, () -> testClass.adjustUserSubscription(userId, request));
    }
}
