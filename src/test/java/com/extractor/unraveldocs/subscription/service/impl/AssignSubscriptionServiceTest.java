package com.extractor.unraveldocs.subscription.service.impl;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionStatus;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AssignSubscriptionServiceTest {

    @Mock
    private SubscriptionPlanRepository planRepository;

    @InjectMocks
    private AssignSubscriptionService assignSubscriptionService;

    private User createUserWithRole(Role role) {
        User user = new User();
        user.setId("test-user-id");
        user.setEmail("test@example.com");
        user.setRole(role);
        return user;
    }

    private SubscriptionPlan createPlan(SubscriptionPlans planName) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(String.valueOf(1L));
        plan.setName(planName);
        return plan;
    }

    @ParameterizedTest
    @CsvSource({
            "SUPER_ADMIN, ENTERPRISE_YEARLY",
            "ADMIN, ENTERPRISE_YEARLY",
            "MODERATOR, PREMIUM_YEARLY",
            "USER, FREE"
    })
    void assignDefaultSubscription_successForAllRoles(String roleStr, String planNameStr) {
        // Arrange
        Role role = Role.valueOf(roleStr);
        SubscriptionPlans planName = SubscriptionPlans.valueOf(planNameStr);

        User user = createUserWithRole(role);
        SubscriptionPlan plan = createPlan(planName);

        when(planRepository.findByName(planName)).thenReturn(Optional.of(plan));

        // Act
        UserSubscription subscription = assignSubscriptionService.assignDefaultSubscription(user);

        // Assert
        assertNotNull(subscription);
        assertEquals(user, subscription.getUser());
        assertEquals(plan, subscription.getPlan());
        assertEquals(SubscriptionStatus.ACTIVE.getStatusName(), subscription.getStatus());
        assertFalse(subscription.isHasUsedTrial());

        verify(planRepository).findByName(planName);
    }

    @Test
    void assignDefaultSubscription_whenPlanNotFound_returnsNull() {
        // Arrange
        User user = createUserWithRole(Role.USER);
        SubscriptionPlans expectedPlanName = SubscriptionPlans.FREE;

        when(planRepository.findByName(expectedPlanName)).thenReturn(Optional.empty());

        // Act
        UserSubscription subscription = assignSubscriptionService.assignDefaultSubscription(user);

        // Assert
        assertNull(subscription);
        verify(planRepository).findByName(expectedPlanName);
    }
}