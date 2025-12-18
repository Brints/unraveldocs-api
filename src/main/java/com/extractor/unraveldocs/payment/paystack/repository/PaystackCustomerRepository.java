package com.extractor.unraveldocs.payment.paystack.repository;

import com.extractor.unraveldocs.payment.paystack.model.PaystackCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PaystackCustomer entity
 */
@Repository
public interface PaystackCustomerRepository extends JpaRepository<PaystackCustomer, String> {

    Optional<PaystackCustomer> findByUserId(String userId);

    Optional<PaystackCustomer> findByCustomerCode(String customerCode);

    Optional<PaystackCustomer> findByEmail(String email);

    boolean existsByUserId(String userId);

    boolean existsByCustomerCode(String customerCode);
}

