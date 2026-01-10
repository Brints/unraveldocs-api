package com.extractor.unraveldocs.subscription.repository;

import com.extractor.unraveldocs.subscription.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {

    Optional<UserSubscription> findByUserId(String userId);

    Optional<UserSubscription> findByPaymentGatewaySubscriptionId(String paymentGatewaySubscriptionId);

    /**
     * Find subscriptions expiring within a date range where auto-renew is disabled.
     */
    List<UserSubscription> findByCurrentPeriodEndBetweenAndAutoRenewFalse(
            OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find subscriptions in trial that expire within a date range.
     */
    List<UserSubscription> findByTrialEndsAtBetweenAndStatusEquals(
            OffsetDateTime startDate, OffsetDateTime endDate, String status);
}