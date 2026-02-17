package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.payment.paypal.model.PayPalCustomer;
import com.extractor.unraveldocs.payment.paypal.repository.PayPalCustomerRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Service for managing PayPal customers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalCustomerService {

    private final PayPalCustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get or create a PayPal customer for the user.
     * Note: PayPal doesn't have a dedicated customer creation API like Stripe.
     * Customers are implicitly created when they make a payment.
     */
    @Transactional
    public PayPalCustomer getOrCreateCustomer(User user) {
        return customerRepository.findByUserId(user.getId())
                .orElseGet(() -> createCustomer(user));
    }

    /**
     * Create a local PayPal customer record.
     */
    @Transactional
    public PayPalCustomer createCustomer(User user) {

        PayPalCustomer customer = PayPalCustomer.builder()
                .user(user)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();

        PayPalCustomer savedCustomer = customerRepository.save(customer);
        log.info("Created PayPal customer record: {}", savedCustomer.getId());

        return savedCustomer;
    }

    /**
     * Update customer with PayPal payer information after first successful payment.
     */
    @Transactional
    public PayPalCustomer updateFromPayerInfo(String userId, String payerId, String email,
            String firstName, String lastName) {
        PayPalCustomer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Customer not found for user: " + userId));

        customer.setPayerId(payerId);
        if (email != null)
            customer.setEmail(email);
        if (firstName != null)
            customer.setFirstName(firstName);
        if (lastName != null)
            customer.setLastName(lastName);

        PayPalCustomer updatedCustomer = customerRepository.save(customer);
        log.info("Updated PayPal customer {} with payer ID: {}", customer.getId(), payerId);

        return updatedCustomer;
    }

    /**
     * Get customer by user ID.
     */
    public Optional<PayPalCustomer> getByUserId(String userId) {
        return customerRepository.findByUserId(userId);
    }

    /**
     * Get customer by PayPal payer ID.
     */
    public Optional<PayPalCustomer> getByPayerId(String payerId) {
        return customerRepository.findByPayerId(payerId);
    }

    /**
     * Update customer metadata.
     */
    @Transactional
    public void updateMetadata(String customerId, Map<String, String> metadata) {
        customerRepository.findById(customerId).ifPresent(customer -> {
            try {
                String metadataJson = objectMapper.writeValueAsString(metadata);
                customer.setMetadata(metadataJson);
                customerRepository.save(customer);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize customer metadata: {}", e.getMessage());
            }
        });
    }

    /**
     * Check if user has a PayPal customer record.
     */
    public boolean customerExists(String userId) {
        return customerRepository.existsByUserId(userId);
    }
}
