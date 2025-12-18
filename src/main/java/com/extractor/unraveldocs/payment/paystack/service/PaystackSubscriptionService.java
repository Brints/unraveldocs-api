package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.paystack.config.PaystackConfig;
import com.extractor.unraveldocs.payment.paystack.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.paystack.dto.response.PaystackResponse;
import com.extractor.unraveldocs.payment.paystack.dto.response.PlanData;
import com.extractor.unraveldocs.payment.paystack.dto.response.SubscriptionData;
import com.extractor.unraveldocs.payment.paystack.exception.PaystackPaymentException;
import com.extractor.unraveldocs.payment.paystack.exception.PaystackSubscriptionNotFoundException;
import com.extractor.unraveldocs.payment.paystack.model.PaystackCustomer;
import com.extractor.unraveldocs.payment.paystack.model.PaystackSubscription;
import com.extractor.unraveldocs.payment.paystack.repository.PaystackSubscriptionRepository;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing Paystack subscriptions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackSubscriptionService {

    private final RestClient paystackRestClient;
    private final PaystackConfig paystackConfig;
    private final PaystackCustomerService customerService;
    private final PaystackSubscriptionRepository subscriptionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitize;

    /**
     * Create a subscription for a user
     */
    @Transactional
    public PaystackSubscription createSubscription(User user, CreateSubscriptionRequest request) {
        try {
            PaystackCustomer customer = customerService.getOrCreateCustomer(user);
            
            SubscriptionPlans planEnum = SubscriptionPlans.fromString(request.getPlanName());
            SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findByName(planEnum)
                    .orElseThrow(() -> new PaystackPaymentException("Subscription plan not found: " + request.getPlanName()));

            String paystackPlanCode = ensurePaystackPlanExists(subscriptionPlan);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("customer", customer.getCustomerCode());
            requestBody.put("plan", paystackPlanCode);

            if (request.getAuthorization() != null) {
                requestBody.put("authorization", request.getAuthorization());
            }

            if (request.getStartDate() != null) {
                requestBody.put("start_date", request.getStartDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }

            String responseBody = paystackRestClient.post()
                    .uri("/subscription")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<SubscriptionData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to create subscription: " + response.getMessage());
            }

            SubscriptionData data = response.getData();

            PaystackSubscription subscription = PaystackSubscription.builder()
                    .user(user)
                    .paystackCustomer(customer)
                    .planCode(paystackPlanCode)
                    .paystackSubscriptionId(data.getId())
                    .subscriptionCode(data.getSubscriptionCode())
                    .emailToken(data.getEmailToken())
                    .status(data.getStatus())
                    .amount(subscriptionPlan.getPrice())
                    .cronExpression(data.getCronExpression())
                    .nextPaymentDate(parsePaystackDateTime(data.getNextPaymentDate()))
                    .invoiceLimit(data.getInvoiceLimit())
                    .paymentsCount(data.getPaymentsCount())
                    .build();

            if (data.getAuthorization() != null) {
                subscription.setAuthorizationCode(data.getAuthorization().getAuthorizationCode());
            }

            PaystackSubscription savedSubscription = subscriptionRepository.save(subscription);
            log.info(
                    "Created Paystack subscription {} for user {}",
                    sanitize.sanitizeLogging(savedSubscription.getSubscriptionCode()),
                    sanitize.sanitizeLogging(user.getId()));

            // Update user subscription record
            updateUserSubscription(user, savedSubscription, subscriptionPlan);

            return savedSubscription;
        } catch (Exception e) {
            log.error("Failed to create subscription for user {}: {}", sanitize.sanitizeLogging(user.getId()), e.getMessage());
            throw new PaystackPaymentException("Failed to create subscription", e);
        }
    }

    /**
     * Ensure plan exists on Paystack, return the plan code.
     * Uses stored paystackPlanCode if available, otherwise creates the plan.
     */
    private String ensurePaystackPlanExists(SubscriptionPlan plan) {
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
        log.info("Created and stored Paystack plan code {} for plan {}", sanitize.sanitizeLogging(newPlanCode), plan.getName().getPlanName());
        return newPlanCode;
    }

    private String createPaystackPlan(SubscriptionPlan plan) {
        try {
            Map<String, Object> requestBody = getStringObjectMap(plan);
            String responseBody = paystackRestClient.post()
                    .uri("/plan")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<PlanData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );
            
            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to create Paystack plan: " + response.getMessage());
            }
            
            return response.getData().getPlanCode();
        } catch (Exception e) {
            log.error("Failed to create Paystack plan: {}", e.getMessage());
            throw new PaystackPaymentException("Failed to create Paystack plan", e);
        }
    }

    private @NonNull Map<String, Object> getStringObjectMap(SubscriptionPlan plan) {
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

    /**
     * Get subscription details from Paystack
     */
    public SubscriptionData getSubscriptionFromPaystack(String subscriptionCode) {
        try {
            String responseBody = paystackRestClient.get()
                    .uri("/subscription/{subscription_code}", subscriptionCode)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<SubscriptionData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to fetch subscription: " + response.getMessage());
            }

            return response.getData();
        } catch (Exception e) {
            log.error("Failed to fetch subscription {}: {}", sanitize.sanitizeLogging(subscriptionCode), e.getMessage());
            throw new PaystackPaymentException("Failed to fetch subscription", e);
        }
    }

    /**
     * Enable a subscription
     */
    @Transactional
    public PaystackSubscription enableSubscription(String subscriptionCode, String emailToken) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("code", subscriptionCode);
            requestBody.put("token", emailToken);

            String responseBody = paystackRestClient.post()
                    .uri("/subscription/enable")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<Object> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to enable subscription: " + response.getMessage());
            }

            PaystackSubscription subscription = subscriptionRepository.findBySubscriptionCode(subscriptionCode)
                    .orElseThrow(() -> new PaystackSubscriptionNotFoundException("Subscription not found: " + subscriptionCode));

            subscription.setStatus("active");
            PaystackSubscription savedSubscription = subscriptionRepository.save(subscription);

            // Update user subscription
            updateUserSubscriptionStatus(subscription.getUser().getId(), "active");

            log.info("Enabled subscription: {}", sanitize.sanitizeLogging(subscriptionCode));
            return savedSubscription;
        } catch (Exception e) {
            log.error("Failed to enable subscription {}: {}", sanitize.sanitizeLogging(subscriptionCode), e.getMessage());
            throw new PaystackPaymentException("Failed to enable subscription", e);
        }
    }

    /**
     * Disable a subscription (cancel at period end)
     */
    @Transactional
    public PaystackSubscription disableSubscription(String subscriptionCode, String emailToken) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("code", subscriptionCode);
            requestBody.put("token", emailToken);

            String responseBody = paystackRestClient.post()
                    .uri("/subscription/disable")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<Object> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to disable subscription: " + response.getMessage());
            }

            PaystackSubscription subscription = subscriptionRepository.findBySubscriptionCode(subscriptionCode)
                    .orElseThrow(() -> new PaystackSubscriptionNotFoundException("Subscription not found: " + subscriptionCode));

            subscription.setStatus("non-renewing");
            PaystackSubscription savedSubscription = subscriptionRepository.save(subscription);

            // Update user subscription
            updateUserSubscriptionStatus(subscription.getUser().getId(), "non-renewing");

            log.info("Disabled subscription: {}", sanitize.sanitizeLogging(subscriptionCode));
            return savedSubscription;
        } catch (Exception e) {
            log.error("Failed to disable subscription {}: {}", sanitize.sanitizeLogging(subscriptionCode), e.getMessage());
            throw new PaystackPaymentException("Failed to disable subscription", e);
        }
    }

    /**
     * Get subscription by code from local database
     */
    public Optional<PaystackSubscription> getSubscriptionByCode(String subscriptionCode) {
        return subscriptionRepository.findBySubscriptionCode(subscriptionCode);
    }

    /**
     * Get active subscription for a user
     */
    public Optional<PaystackSubscription> getActiveSubscriptionByUserId(String userId) {
        return subscriptionRepository.findActiveSubscriptionByUserId(userId);
    }

    /**
     * Get all subscriptions for a user
     */
    public Page<PaystackSubscription> getSubscriptionsByUserId(String userId, Pageable pageable) {
        return subscriptionRepository.findByUser_Id(userId, pageable);
    }

    /**
     * Get subscriptions by status
     */
    public List<PaystackSubscription> getSubscriptionsByStatus(String status) {
        return subscriptionRepository.findByStatus(status);
    }

    /**
     * Update subscription from webhook data
     */
    @Transactional
    public void updateSubscriptionFromWebhook(SubscriptionData subscriptionData) {
        subscriptionRepository.findBySubscriptionCode(subscriptionData.getSubscriptionCode())
                .ifPresent(subscription -> {
                    subscription.setStatus(subscriptionData.getStatus());
                    subscription.setNextPaymentDate(parsePaystackDateTime(subscriptionData.getNextPaymentDate()));
                    subscription.setPaymentsCount(subscriptionData.getPaymentsCount());

                    if (subscriptionData.getCancelledAt() != null) {
                        subscription.setCancelledAt(parsePaystackDateTime(subscriptionData.getCancelledAt()));
                    }

                    subscriptionRepository.save(subscription);

                    // Update user subscription
                    updateUserSubscriptionStatus(subscription.getUser().getId(), subscriptionData.getStatus());

                    log.info("Updated subscription {} from webhook", sanitize.sanitizeLogging(subscriptionData.getSubscriptionCode()));
                });
    }

    /**
     * Update user subscription record
     */
    private void updateUserSubscription(User user, PaystackSubscription paystackSubscription, SubscriptionPlan plan) {
        Optional<UserSubscription> existingSubscription = userSubscriptionRepository.findByUserId(user.getId());

        UserSubscription userSubscription;
        if (existingSubscription.isPresent()) {
            userSubscription = existingSubscription.get();
        } else {
            userSubscription = new UserSubscription();
            userSubscription.setUser(user);
        }

        // Link to the subscription plan
        userSubscription.setPlan(plan);
        userSubscription.setPaymentGatewaySubscriptionId(paystackSubscription.getSubscriptionCode());
        userSubscription.setStatus(paystackSubscription.getStatus());
        userSubscription.setCurrentPeriodStart(OffsetDateTime.now());
        userSubscription.setCurrentPeriodEnd(paystackSubscription.getNextPaymentDate());
        userSubscription.setAutoRenew(!"non-renewing".equals(paystackSubscription.getStatus()));

        userSubscriptionRepository.save(userSubscription);
    }

    /**
     * Update user subscription status
     */
    private void updateUserSubscriptionStatus(String userId, String status) {
        userSubscriptionRepository.findByUserId(userId).ifPresent(userSubscription -> {
            userSubscription.setStatus(status);
            userSubscription.setAutoRenew(!"non-renewing".equals(status) && !"cancelled".equals(status));
            userSubscriptionRepository.save(userSubscription);
        });
    }

    /**
     * Parse Paystack datetime string
     */
    private OffsetDateTime parsePaystackDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", sanitize.sanitizeLogging(dateTimeString));
            return null;
        }
    }
}
