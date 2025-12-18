package com.extractor.unraveldocs.payment.stripe.repository;

import com.extractor.unraveldocs.payment.enums.SubscriptionStatus;
import com.extractor.unraveldocs.payment.stripe.model.StripeSubscription;
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
 * Repository for StripeSubscription entity operations.
 */
@Repository
public interface StripeSubscriptionRepository extends JpaRepository<StripeSubscription, String> {

    /**
     * Find subscription by Stripe subscription ID
     */
    Optional<StripeSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * Find all subscriptions for a user
     */
    Page<StripeSubscription> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find subscriptions by user ID (without pagination)
     */
    List<StripeSubscription> findByUserId(String userId);

    /**
     * Find active subscription for a user
     */
    @Query("SELECT s FROM StripeSubscription s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'TRIALING')")
    Optional<StripeSubscription> findActiveByUserId(@Param("userId") String userId);

    /**
     * Find all active subscriptions for a user
     */
    @Query("SELECT s FROM StripeSubscription s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'TRIALING')")
    List<StripeSubscription> findAllActiveByUserId(@Param("userId") String userId);

    /**
     * Find subscriptions by status
     */
    List<StripeSubscription> findByStatus(SubscriptionStatus status);

    /**
     * Find subscriptions expiring before a date
     */
    @Query("SELECT s FROM StripeSubscription s WHERE s.currentPeriodEnd < :date AND s.status = 'ACTIVE'")
    List<StripeSubscription> findExpiringBefore(@Param("date") OffsetDateTime date);

    /**
     * Find subscriptions with pending cancellation
     */
    List<StripeSubscription> findByCancelAtPeriodEndTrue();

    /**
     * Check if subscription exists by Stripe subscription ID
     */
    boolean existsByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * Count active subscriptions for a user
     */
    @Query("SELECT COUNT(s) FROM StripeSubscription s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'TRIALING')")
    long countActiveByUserId(@Param("userId") String userId);

    /**
     * Find subscriptions by customer ID
     */
    List<StripeSubscription> findByStripeCustomerId(String stripeCustomerId);
}
