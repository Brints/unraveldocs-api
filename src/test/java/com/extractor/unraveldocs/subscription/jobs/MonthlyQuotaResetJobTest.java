package com.extractor.unraveldocs.subscription.jobs;

import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyQuotaResetJobTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private MonthlyQuotaResetJob monthlyQuotaResetJob;

    private UserSubscription testSubscription;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");

        testSubscription = new UserSubscription();
        testSubscription.setId("sub-123");
        testSubscription.setUser(testUser);
        testSubscription.setMonthlyDocumentsUploaded(15);
        testSubscription.setOcrPagesUsed(20);
        testSubscription.setQuotaResetDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)); // Past due for reset
    }

    @Test
    @DisplayName("Should reset monthly quotas when subscription is due for reset")
    void shouldResetMonthlyQuotasWhenDue() {
        // Given
        when(userSubscriptionRepository.findSubscriptionsNeedingQuotaReset(any(OffsetDateTime.class)))
                .thenReturn(List.of(testSubscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        monthlyQuotaResetJob.resetMonthlyQuotas();

        // Then
        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(userSubscriptionRepository).save(captor.capture());

        UserSubscription savedSubscription = captor.getValue();
        assertThat(savedSubscription.getMonthlyDocumentsUploaded()).isZero();
        assertThat(savedSubscription.getOcrPagesUsed()).isZero();
        assertThat(savedSubscription.getQuotaResetDate()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Should not process when no subscriptions need reset")
    void shouldNotProcessWhenNoSubscriptionsNeedReset() {
        // Given
        when(userSubscriptionRepository.findSubscriptionsNeedingQuotaReset(any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        monthlyQuotaResetJob.resetMonthlyQuotas();

        // Then
        verify(userSubscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set next reset date to first day of next month")
    void shouldSetNextResetDateToFirstDayOfNextMonth() {
        // Given
        when(userSubscriptionRepository.findSubscriptionsNeedingQuotaReset(any(OffsetDateTime.class)))
                .thenReturn(List.of(testSubscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime expectedResetDate = OffsetDateTime.now(ZoneOffset.UTC)
                .with(TemporalAdjusters.firstDayOfNextMonth())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        // When
        monthlyQuotaResetJob.resetMonthlyQuotas();

        // Then
        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(userSubscriptionRepository).save(captor.capture());

        UserSubscription savedSubscription = captor.getValue();
        assertThat(savedSubscription.getQuotaResetDate()).isEqualTo(expectedResetDate);
    }

    @Test
    @DisplayName("Should initialize quota reset dates for subscriptions without one")
    void shouldInitializeQuotaResetDatesForSubscriptionsWithoutOne() {
        // Given
        UserSubscription subscriptionWithoutResetDate = new UserSubscription();
        subscriptionWithoutResetDate.setId("sub-456");
        subscriptionWithoutResetDate.setQuotaResetDate(null);

        when(userSubscriptionRepository.findSubscriptionsWithoutQuotaResetDate())
                .thenReturn(List.of(subscriptionWithoutResetDate));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        monthlyQuotaResetJob.initializeQuotaResetDates();

        // Then
        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(userSubscriptionRepository).save(captor.capture());

        UserSubscription savedSubscription = captor.getValue();
        assertThat(savedSubscription.getQuotaResetDate()).isNotNull();
        assertThat(savedSubscription.getQuotaResetDate()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Should handle null values in subscription gracefully during reset")
    void shouldHandleNullValuesInSubscriptionGracefully() {
        // Given
        testSubscription.setMonthlyDocumentsUploaded(null);
        testSubscription.setOcrPagesUsed(null);

        when(userSubscriptionRepository.findSubscriptionsNeedingQuotaReset(any(OffsetDateTime.class)))
                .thenReturn(List.of(testSubscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        monthlyQuotaResetJob.resetMonthlyQuotas();

        // Then
        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(userSubscriptionRepository).save(captor.capture());

        UserSubscription savedSubscription = captor.getValue();
        assertThat(savedSubscription.getMonthlyDocumentsUploaded()).isZero();
        assertThat(savedSubscription.getOcrPagesUsed()).isZero();
    }
}
