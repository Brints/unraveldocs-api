package com.extractor.unraveldocs.payment.paystack.repository;

import com.extractor.unraveldocs.payment.paystack.model.PaystackSubscription;
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
 * Repository for PaystackSubscription entity
 */
@Repository
public interface PaystackSubscriptionRepository extends JpaRepository<PaystackSubscription, String> {

    Optional<PaystackSubscription> findBySubscriptionCode(String subscriptionCode);

    Optional<PaystackSubscription> findByPaystackSubscriptionId(Long paystackSubscriptionId);

    List<PaystackSubscription> findByUser_Id(String userId);

    Optional<PaystackSubscription> findByUser_IdAndStatus(String userId, String status);

    Page<PaystackSubscription> findByUser_Id(String userId, Pageable pageable);

    List<PaystackSubscription> findByStatus(String status);

    @Query("SELECT s FROM PaystackSubscription s WHERE s.status = 'active' AND s.nextPaymentDate <= :date")
    List<PaystackSubscription> findSubscriptionsDueForRenewal(@Param("date") OffsetDateTime date);

    @Query("SELECT s FROM PaystackSubscription s WHERE s.user.id = :userId AND s.status = 'active'")
    Optional<PaystackSubscription> findActiveSubscriptionByUserId(@Param("userId") String userId);

    boolean existsBySubscriptionCode(String subscriptionCode);

    @Query("SELECT COUNT(s) FROM PaystackSubscription s WHERE s.status = 'active'")
    Long countActiveSubscriptions();
}

