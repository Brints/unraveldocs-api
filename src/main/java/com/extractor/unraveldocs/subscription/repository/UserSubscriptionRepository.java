package com.extractor.unraveldocs.subscription.repository;

import com.extractor.unraveldocs.subscription.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {

        Optional<UserSubscription> findByUserId(String userId);

        @Query("SELECT us FROM UserSubscription us JOIN FETCH us.plan WHERE us.user.id = :userId")
        Optional<UserSubscription> findByUserIdWithPlan(@Param("userId") String userId);

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

        // ========== Coupon Notification Query Methods (Performance Optimized)
        // ==========

        /**
         * Find all active paid subscriptions (non-FREE plans with ACTIVE status).
         * Used for targeting paid users with coupons.
         */
        @Query("SELECT us FROM UserSubscription us JOIN FETCH us.plan p JOIN FETCH us.user " +
                        "WHERE UPPER(us.status) = 'ACTIVE' AND p.name <> com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans.FREE")
        List<UserSubscription> findActivePaidSubscriptions();

        /**
         * Find active subscriptions by plan name.
         * Used for targeting users on specific plans with coupons.
         */
        @Query("SELECT us FROM UserSubscription us JOIN FETCH us.plan p JOIN FETCH us.user " +
                        "WHERE UPPER(us.status) = 'ACTIVE' AND UPPER(CAST(p.name AS string)) = UPPER(:planName)")
        List<UserSubscription> findActiveSubscriptionsByPlanName(@Param("planName") String planName);

        /**
         * Find free tier users with high activity (OCR pages used >= 20).
         * Used for targeting engaged free users with upgrade coupons.
         */
        @Query("SELECT us FROM UserSubscription us JOIN FETCH us.plan p JOIN FETCH us.user " +
                        "WHERE p.name = com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans.FREE " +
                        "AND us.ocrPagesUsed IS NOT NULL AND us.ocrPagesUsed >= 20")
        List<UserSubscription> findFreeTierWithHighActivity();

        /**
         * Find recently expired subscriptions (expired within 3 months).
         * Used for targeting churned users with win-back coupons.
         */
        @Query("SELECT us FROM UserSubscription us JOIN FETCH us.user " +
                        "WHERE UPPER(us.status) <> 'ACTIVE' " +
                        "AND us.currentPeriodEnd IS NOT NULL AND us.currentPeriodEnd > :threeMonthsAgo")
        List<UserSubscription> findRecentlyExpiredSubscriptions(@Param("threeMonthsAgo") OffsetDateTime threeMonthsAgo);

        /**
         * Find high activity subscriptions (using > 50% of plan's OCR page limit).
         * Used for targeting power users with retention coupons.
         */
        @Query("SELECT us FROM UserSubscription us JOIN FETCH us.plan p JOIN FETCH us.user " +
                        "WHERE us.ocrPagesUsed IS NOT NULL AND p.ocrPageLimit IS NOT NULL AND p.ocrPageLimit > 0 " +
                        "AND (CAST(us.ocrPagesUsed AS double) / CAST(p.ocrPageLimit AS double)) > 0.5")
        List<UserSubscription> findHighActivitySubscriptions();

        // ========== Monthly Quota Reset Query Methods ==========

        /**
         * Find subscriptions that need quota reset (quota_reset_date <= now).
         * Used by the scheduled quota reset job.
         */
        @Query("SELECT us FROM UserSubscription us WHERE us.quotaResetDate IS NOT NULL AND us.quotaResetDate <= :now")
        List<UserSubscription> findSubscriptionsNeedingQuotaReset(@Param("now") OffsetDateTime now);

        /**
         * Find subscriptions without a quota reset date set.
         * Used to initialize quota reset dates for existing subscriptions.
         */
        @Query("SELECT us FROM UserSubscription us WHERE us.quotaResetDate IS NULL")
        List<UserSubscription> findSubscriptionsWithoutQuotaResetDate();

        // ========== Dashboard KPI Query Methods ==========

        long countByStatusIgnoreCase(String status);

        @Query("SELECT p.name AS planName, COUNT(us) AS userCount FROM UserSubscription us JOIN us.plan p GROUP BY p.name")
        List<Object[]> countUsersByPlan();

        @Query("SELECT us.status AS status, COUNT(us) AS userCount FROM UserSubscription us GROUP BY us.status")
        List<Object[]> countUsersByStatus();

        @Query("SELECT SUM(us.storageUsed) FROM UserSubscription us WHERE us.storageUsed IS NOT NULL")
        Long sumTotalStorageUsed();

        // ========== Phase 3A Admin Dashboard Stats Queries ==========

        @Query("SELECT us.subscriptionSource AS source, COUNT(us) AS sourceCount FROM UserSubscription us GROUP BY us.subscriptionSource")
        List<Object[]> countBySubscriptionSourceGrouped();

        /**
         * Calculate MRR (Monthly Recurring Revenue) by summing prices of active plans.
         * Note: Yearly plans price division by 12 should preferably be handled in the service layer,
         * or this query can be adjusted to sum up Monthly-equivalent prices.
         */
        @Query("SELECT p.billingIntervalValue, p.billingIntervalUnit, SUM(p.price) " +
               "FROM UserSubscription us JOIN us.plan p " +
               "WHERE UPPER(us.status) = 'ACTIVE' AND p.price IS NOT NULL AND p.price > 0 " +
               "GROUP BY p.billingIntervalValue, p.billingIntervalUnit")
        List<Object[]> sumPricesForActivePaidSubscriptionsGroupedByInterval();

        long countByHasUsedTrialTrueAndStatusIgnoreCase(String status);
        
        long countByStatusInIgnoreCase(List<String> statuses);

        // Quota near-limit warnings (>80% usage)
        @Query("SELECT COUNT(us) FROM UserSubscription us JOIN us.plan p " +
               "WHERE us.storageUsed IS NOT NULL AND p.storageLimit IS NOT NULL AND p.storageLimit > 0 " +
               "AND (CAST(us.storageUsed AS double) / CAST(p.storageLimit AS double)) > 0.8")
        long countUsersNearStorageLimit();

        @Query("SELECT COUNT(us) FROM UserSubscription us JOIN us.plan p " +
               "WHERE us.ocrPagesUsed IS NOT NULL AND p.ocrPageLimit IS NOT NULL AND p.ocrPageLimit > 0 " +
               "AND (CAST(us.ocrPagesUsed AS double) / CAST(p.ocrPageLimit AS double)) > 0.8")
        long countUsersNearOcrLimit();

        @Query("SELECT COUNT(us) FROM UserSubscription us JOIN us.plan p " +
               "WHERE us.monthlyDocumentsUploaded IS NOT NULL AND p.documentUploadLimit IS NOT NULL AND p.documentUploadLimit > 0 " +
               "AND (CAST(us.monthlyDocumentsUploaded AS double) / CAST(p.documentUploadLimit AS double)) > 0.8")
        long countUsersNearDocLimit();

        @Query("SELECT COUNT(us) FROM UserSubscription us JOIN us.plan p " +
               "WHERE us.aiOperationsUsed IS NOT NULL AND p.aiOperationsLimit IS NOT NULL AND p.aiOperationsLimit > 0 " +
               "AND (CAST(us.aiOperationsUsed AS double) / CAST(p.aiOperationsLimit AS double)) > 0.8")
        long countUsersNearAiLimit();

        @Query("SELECT COUNT(us) FROM UserSubscription us JOIN us.plan p " +
               "WHERE UPPER(us.status) = 'ACTIVE' AND p.name <> com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans.FREE")
        long countActivePaidSubscriptions();

        // ========== Phase 3C Plan Subscribers Query ==========

        @Query("SELECT us FROM UserSubscription us JOIN FETCH us.user WHERE us.plan.id = :planId " +
               "ORDER BY us.createdAt DESC")
        org.springframework.data.domain.Page<UserSubscription> findByPlanIdWithUser(@Param("planId") String planId, org.springframework.data.domain.Pageable pageable);
}