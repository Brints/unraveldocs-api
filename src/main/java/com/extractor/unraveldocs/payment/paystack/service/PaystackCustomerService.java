package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.paystack.dto.response.CustomerData;
import com.extractor.unraveldocs.payment.paystack.dto.response.PaystackResponse;
import com.extractor.unraveldocs.payment.paystack.exception.PaystackCustomerNotFoundException;
import com.extractor.unraveldocs.payment.paystack.exception.PaystackPaymentException;
import com.extractor.unraveldocs.payment.paystack.model.PaystackCustomer;
import com.extractor.unraveldocs.payment.paystack.repository.PaystackCustomerRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing Paystack customers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackCustomerService {

    private final RestClient paystackRestClient;
    private final PaystackCustomerRepository customerRepository;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitize;

    /**
     * Get or create a Paystack customer for a user
     */
    @Transactional
    public PaystackCustomer getOrCreateCustomer(User user) {
        Optional<PaystackCustomer> existingCustomer = customerRepository.findByUserId(user.getId());

        if (existingCustomer.isPresent()) {
            log.debug("Found existing Paystack customer for user: {}", sanitize.sanitizeLogging(user.getId()));
            return existingCustomer.get();
        }

        return createCustomer(user);
    }

    /**
     * Create a new Paystack customer
     */
    @Transactional
    public PaystackCustomer createCustomer(User user) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", user.getEmail());
            requestBody.put("first_name", user.getFirstName());
            requestBody.put("last_name", user.getLastName());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("user_id", user.getId());
            requestBody.put("metadata", metadata);

            String responseBody = paystackRestClient.post()
                    .uri("/customer")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<CustomerData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to create customer: " + response.getMessage());
            }

            CustomerData customerData = response.getData();

            PaystackCustomer customer = PaystackCustomer.builder()
                    .user(user)
                    .customerCode(customerData.getCustomerCode())
                    .paystackCustomerId(customerData.getId())
                    .email(customerData.getEmail())
                    .firstName(customerData.getFirstName())
                    .lastName(customerData.getLastName())
                    .phone(customerData.getPhone())
                    .build();

            PaystackCustomer savedCustomer = customerRepository.save(customer);
            log.info("Created Paystack customer {} for user {}", sanitize.sanitizeLogging(savedCustomer.getCustomerCode()), sanitize.sanitizeLogging(user.getId()));

            return savedCustomer;
        } catch (Exception e) {
            log.error("Failed to create Paystack customer for user {}: {}", sanitize.sanitizeLogging(user.getId()), e.getMessage());
            throw new PaystackPaymentException("Failed to create Paystack customer", e);
        }
    }

    /**
     * Get customer by user ID
     */
    public PaystackCustomer getCustomerByUserId(String userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new PaystackCustomerNotFoundException("Paystack customer not found for user: " + userId));
    }

    /**
     * Get customer by customer code
     */
    public PaystackCustomer getCustomerByCode(String customerCode) {
        return customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new PaystackCustomerNotFoundException("Paystack customer not found: " + customerCode));
    }

    /**
     * Update customer details from Paystack
     */
    @Transactional
    public PaystackCustomer updateCustomerFromPaystack(String customerCode) {
        try {
            String responseBody = paystackRestClient.get()
                    .uri("/customer/{customer_code}", customerCode)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<CustomerData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to fetch customer: " + response.getMessage());
            }

            CustomerData customerData = response.getData();
            PaystackCustomer customer = getCustomerByCode(customerCode);

            customer.setFirstName(customerData.getFirstName());
            customer.setLastName(customerData.getLastName());
            customer.setPhone(customerData.getPhone());
            customer.setRiskAction(customerData.getRiskAction());

            return customerRepository.save(customer);
        } catch (PaystackCustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update customer {}: {}", customerCode, e.getMessage());
            throw new PaystackPaymentException("Failed to update customer", e);
        }
    }

    /**
     * Check if customer exists for user
     */
    public boolean customerExistsForUser(String userId) {
        return customerRepository.existsByUserId(userId);
    }
}
