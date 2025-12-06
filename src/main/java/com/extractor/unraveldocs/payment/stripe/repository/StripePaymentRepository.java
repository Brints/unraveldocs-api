package com.extractor.unraveldocs.payment.stripe.repository;

import com.extractor.unraveldocs.payment.stripe.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.stripe.model.StripePayment;
import lombok.NonNull;
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
 * Repository for StripePayment entity
 */
@Repository
public interface StripePaymentRepository extends JpaRepository<@NonNull StripePayment, @NonNull String> {
    
    /**
     * Find a payment by payment intent ID
     */
    Optional<StripePayment> findByPaymentIntentId(String paymentIntentId);
    
    /**
     * Find a payment by subscription ID
     */
    List<StripePayment> findBySubscriptionId(String subscriptionId);
    
    /**
     * Find a payment by checkout session ID
     */
    Optional<StripePayment> findByCheckoutSessionId(String checkoutSessionId);
    
    /**
     * Find all payments for a user
     */
    Page<@NonNull StripePayment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * Find payments within a date range
     */
    @Query("SELECT p FROM StripePayment p WHERE p.user.id = :userId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<StripePayment> findPaymentsByUserAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
    
    /**
     * Check if a payment exists by payment intent ID
     */
    boolean existsByPaymentIntentId(String paymentIntentId);
    
    /**
     * Count successful payments for a user
     */
    long countByUserIdAndStatus(String userId, PaymentStatus status);
}
