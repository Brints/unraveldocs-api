package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.paystack.config.PaystackConfig;
import com.extractor.unraveldocs.payment.paystack.dto.response.PaystackResponse;
import com.extractor.unraveldocs.payment.paystack.dto.response.PlanData;
import com.extractor.unraveldocs.payment.paystack.exception.PaystackPaymentException;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing Paystack plan operations.
 * Handles resolution of internal plan codes to Paystack plan codes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackPlanService {

    private final RestClient paystackRestClient;
    private final PaystackConfig paystackConfig;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitize;

    /**
     * Resolve an internal plan code (e.g., "STARTER_MONTHLY") to the actual Paystack plan code (e.g., "PLN_xxx").
     * If the internal plan code is already a Paystack plan code (starts with "PLN_"), it is returned as-is.
     *
     * @param planCode The internal plan code or Paystack plan code
     * @return The Paystack plan code
     * @throws PaystackPaymentException if the plan is not found or cannot be created
     */
    @Transactional
    public String resolvePaystackPlanCode(String planCode) {
        if (planCode == null || planCode.isEmpty()) {
            return null;
        }

        // If it's already a Paystack plan code, return as-is
        if (planCode.startsWith("PLN_")) {
            return planCode;
        }

        try {
            // Try to parse as an internal plan name
            SubscriptionPlans planEnum = parseSubscriptionPlan(planCode);
            SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findByName(planEnum)
                    .orElseThrow(() -> new PaystackPaymentException("Subscription plan not found: " + planCode));

            return ensurePaystackPlanExists(subscriptionPlan);
        } catch (IllegalArgumentException e) {
            throw new PaystackPaymentException("Invalid plan code: " + planCode);
        }
    }

    /**
     * Parse a plan code string to a SubscriptionPlans enum.
     * Handles various formats like "STARTER_MONTHLY", "Starter_Monthly", etc.
     */
    private SubscriptionPlans parseSubscriptionPlan(String planCode) {
        // Try direct enum match first
        try {
            return SubscriptionPlans.valueOf(planCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try fromString method which handles the plan name format
            return SubscriptionPlans.fromString(planCode);
        }
    }

    /**
     * Ensure plan exists on Paystack, return the plan code.
     * Uses stored paystackPlanCode if available, otherwise creates the plan.
     */
    public String ensurePaystackPlanExists(SubscriptionPlan plan) {
        // If we already have the Paystack plan code stored, verify it exists
        if (plan.getPaystackPlanCode() != null && !plan.getPaystackPlanCode().isEmpty()) {
            try {
                paystackRestClient.get()
                        .uri("/plan/{plan_code}", plan.getPaystackPlanCode())
                        .retrieve()
                        .body(String.class);
                return plan.getPaystackPlanCode();
            } catch (Exception e) {
                log.warn(
                        "Stored Paystack plan code {} not found, recreating plan...",
                        sanitize.sanitizeLogging(plan.getPaystackPlanCode()));
            }
        }

        // Create new plan on Paystack and store the code
        String newPlanCode = createPaystackPlan(plan);
        plan.setPaystackPlanCode(newPlanCode);
        subscriptionPlanRepository.save(plan);
        log.info("Created and stored Paystack plan code {} for plan {}",
                sanitize.sanitizeLogging(newPlanCode), plan.getName().getPlanName());
        return newPlanCode;
    }

    /**
     * Create a plan on Paystack and return the generated plan code.
     */
    private String createPaystackPlan(SubscriptionPlan plan) {
        try {
            Map<String, Object> requestBody = buildPlanRequestBody(plan);
            String responseBody = paystackRestClient.post()
                    .uri("/plan")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<PlanData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {
                    });

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to create Paystack plan: " + response.getMessage());
            }

            return response.getData().getPlanCode();
        } catch (PaystackPaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create Paystack plan: {}", e.getMessage());
            throw new PaystackPaymentException("Failed to create Paystack plan", e);
        }
    }

    /**
     * Build the request body for creating a Paystack plan.
     */
    private Map<String, Object> buildPlanRequestBody(SubscriptionPlan plan) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", plan.getName().getPlanName());
        requestBody.put("amount", plan.getPrice().multiply(BigDecimal.valueOf(100)).intValue()); // Convert to kobo

        String interval = switch (plan.getBillingIntervalUnit()) {
            case MONTH -> "monthly";
            case YEAR -> "annually";
            default -> "monthly";
        };
        requestBody.put("interval", interval);
        requestBody.put("currency", plan.getCurrency() != null ? plan.getCurrency().name() : paystackConfig.getDefaultCurrency());
        return requestBody;
    }
}
