package com.extractor.unraveldocs.payment.paypal.repository;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.paypal.model.PayPalPayment;
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
 * Repository for PayPal payment operations.
 */
@Repository
public interface PayPalPaymentRepository extends JpaRepository<PayPalPayment, String> {

    Optional<PayPalPayment> findByOrderId(String orderId);

    Optional<PayPalPayment> findByCaptureId(String captureId);

    Optional<PayPalPayment> findByAuthorizationId(String authorizationId);

    Page<PayPalPayment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<PayPalPayment> findByUserIdAndStatus(String userId, PaymentStatus status, Pageable pageable);

    List<PayPalPayment> findBySubscriptionId(String subscriptionId);

    @Query("SELECT p FROM PayPalPayment p WHERE p.user.id = :userId " +
            "AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<PayPalPayment> findPaymentsByUserAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    long countByUserIdAndStatus(String userId, PaymentStatus status);

    boolean existsByOrderId(String orderId);

    boolean existsByCaptureId(String captureId);
}
