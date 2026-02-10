package com.extractor.unraveldocs.subscription.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSubscriptionServiceImplTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserEmailTemplateService emailTemplateService;

    @Mock
    private SanitizeLogging sanitizer;

    @InjectMocks
    private UserSubscriptionServiceImpl userSubscriptionService;

    private User user;
    private SubscriptionPlan plan;
    private SubscriptionPlan freePlan;
    private UserSubscription subscription;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user123");
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        plan = new SubscriptionPlan();
        plan.setId("plan123");
        plan.setName(SubscriptionPlans.PRO_YEARLY);
        plan.setTrialDays(14);

        freePlan = new SubscriptionPlan();
        freePlan.setId("freePlan");
        freePlan.setName(SubscriptionPlans.FREE);

        subscription = new UserSubscription();
        subscription.setId("sub123");
        subscription.setUser(user);
        subscription.setPlan(freePlan);
        subscription.setStatus("active"); // Free plan is active
    }

    @Test
    void testActivateTrial_SendsNotifications() {
        when(subscriptionPlanRepository.findById("plan123")).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findByUserId("user123")).thenReturn(Optional.of(subscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(subscription);

        userSubscriptionService.activateTrial(user, "plan123");

        verify(notificationService).sendToUser(
                eq("user123"),
                eq(NotificationType.TRIAL_ACTIVATED),
                anyString(),
                anyString(),
                anyMap());

        verify(emailTemplateService).sendTrialActivatedEmail(
                eq("test@example.com"),
                eq("John"),
                eq("Doe"),
                eq("Pro_Yearly"), // plan.getName().getPlanName() for PRO_YEARLY
                anyString());
    }

    @Test
    void testCheckAndExpireTrials_SendsNotifications() {
        subscription.setPlan(plan);
        subscription.setStatus("TRIAL");
        subscription.setTrialEndsAt(OffsetDateTime.now().minusDays(1));

        when(userSubscriptionRepository.findByTrialEndsAtBetweenAndStatusEquals(any(), any(), eq("TRIAL")))
                .thenReturn(List.of(subscription));
        when(subscriptionPlanRepository.findByName(SubscriptionPlans.FREE)).thenReturn(Optional.of(freePlan));
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(subscription);
        when(sanitizer.sanitizeLoggingInteger(any(Integer.class))).thenReturn("1");

        userSubscriptionService.checkAndExpireTrials();

        verify(notificationService).sendToUser(
                eq("user123"),
                eq(NotificationType.TRIAL_EXPIRED),
                anyString(),
                anyString(),
                anyMap());

        verify(emailTemplateService).sendTrialExpiredEmail(
                eq("test@example.com"),
                eq("John"),
                eq("Doe"),
                anyString());
    }
}
