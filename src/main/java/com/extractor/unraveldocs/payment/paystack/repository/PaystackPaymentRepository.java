package com.extractor.unraveldocs.payment.paystack.repository;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.paystack.model.PaystackPayment;
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
 * Repository for PaystackPayment entity
 */
@Repository
public interface PaystackPaymentRepository extends JpaRepository<PaystackPayment, String> {

    Optional<PaystackPayment> findByReference(String reference);

    Optional<PaystackPayment> findByTransactionId(Long transactionId);

    List<PaystackPayment> findBySubscriptionCode(String subscriptionCode);

    Page<PaystackPayment> findByUser_Id(String userId, Pageable pageable);

    Page<PaystackPayment> findByUser_IdAndStatus(String userId, PaymentStatus status, Pageable pageable);

    List<PaystackPayment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM PaystackPayment p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<PaystackPayment> findRecentPaymentsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT p FROM PaystackPayment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<PaystackPayment> findPaymentsBetweenDates(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    boolean existsByReference(String reference);

    @Query("SELECT SUM(p.amount) FROM PaystackPayment p WHERE p.user.id = :userId AND p.status = 'SUCCEEDED'")
    Long getTotalAmountByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(p) FROM PaystackPayment p WHERE p.user.id = :userId AND p.status = 'SUCCEEDED'")
    Long getSuccessfulPaymentCountByUserId(@Param("userId") String userId);
}

