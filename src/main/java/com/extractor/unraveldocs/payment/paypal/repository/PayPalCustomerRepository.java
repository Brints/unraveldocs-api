package com.extractor.unraveldocs.payment.paypal.repository;

import com.extractor.unraveldocs.payment.paypal.model.PayPalCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PayPal customer operations.
 */
@Repository
public interface PayPalCustomerRepository extends JpaRepository<PayPalCustomer, String> {

    Optional<PayPalCustomer> findByUserId(String userId);

    Optional<PayPalCustomer> findByPayerId(String payerId);

    Optional<PayPalCustomer> findByEmail(String email);

    boolean existsByUserId(String userId);

    boolean existsByPayerId(String payerId);
}
