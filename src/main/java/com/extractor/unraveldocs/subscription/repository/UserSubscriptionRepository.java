package com.extractor.unraveldocs.subscription.repository;

import com.extractor.unraveldocs.subscription.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {

    Optional<UserSubscription> findByUserId(String userId);

    Optional<UserSubscription> findByPaymentGatewaySubscriptionId(String paymentGatewaySubscriptionId);
}