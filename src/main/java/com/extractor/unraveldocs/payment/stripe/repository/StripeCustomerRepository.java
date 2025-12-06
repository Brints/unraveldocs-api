package com.extractor.unraveldocs.payment.stripe.repository;

import com.extractor.unraveldocs.payment.stripe.model.StripeCustomer;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for StripeCustomer entity
 */
@Repository
public interface StripeCustomerRepository extends JpaRepository<StripeCustomer, String> {
    
    /**
     * Find a Stripe customer by user
     */
    Optional<StripeCustomer> findByUser(User user);
    
    /**
     * Find a Stripe customer by user ID
     */
    Optional<StripeCustomer> findByUserId(String userId);
    
    /**
     * Find a Stripe customer by Stripe customer ID
     */
    Optional<StripeCustomer> findByStripeCustomerId(String stripeCustomerId);
}
