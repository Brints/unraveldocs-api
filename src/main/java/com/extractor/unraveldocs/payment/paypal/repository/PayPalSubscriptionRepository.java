package com.extractor.unraveldocs.payment.paypal.repository;

import com.extractor.unraveldocs.payment.paypal.model.PayPalSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PayPal subscription operations.
 */
@Repository
public interface PayPalSubscriptionRepository extends JpaRepository<PayPalSubscription, String> {

    Optional<PayPalSubscription> findBySubscriptionId(String subscriptionId);

    Page<PayPalSubscription> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<PayPalSubscription> findByUserIdAndStatus(String userId, String status);

    @Query("SELECT s FROM PayPalSubscription s WHERE s.user.id = :userId AND s.status = 'ACTIVE'")
    Optional<PayPalSubscription> findActiveSubscriptionByUserId(@Param("userId") String userId);

    @Query("SELECT s FROM PayPalSubscription s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'SUSPENDED')")
    List<PayPalSubscription> findActiveOrSuspendedByUserId(@Param("userId") String userId);

    List<PayPalSubscription> findByStatus(String status);

    @Query("SELECT s FROM PayPalSubscription s WHERE s.status = 'ACTIVE' AND s.nextBillingTime BETWEEN :now AND :endTime")
    List<PayPalSubscription> findSubscriptionsWithUpcomingBilling(
            @Param("now") OffsetDateTime now,
            @Param("endTime") OffsetDateTime endTime);

    @Query("SELECT s FROM PayPalSubscription s WHERE s.planId = :planId AND s.status = 'ACTIVE'")
    List<PayPalSubscription> findActiveByPlanId(@Param("planId") String planId);

    boolean existsBySubscriptionId(String subscriptionId);

    long countByUserIdAndStatus(String userId, String status);
}
