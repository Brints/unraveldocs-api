package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.payment.paypal.config.PayPalConfig;
import com.extractor.unraveldocs.payment.paypal.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalSubscriptionResponse;
import com.extractor.unraveldocs.payment.paypal.exception.PayPalPaymentException;
import com.extractor.unraveldocs.payment.paypal.exception.PayPalSubscriptionNotFoundException;
import com.extractor.unraveldocs.payment.paypal.model.PayPalCustomer;
import com.extractor.unraveldocs.payment.paypal.model.PayPalSubscription;
import com.extractor.unraveldocs.payment.paypal.repository.PayPalSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for PayPal subscription operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalSubscriptionService {

    private final PayPalConfig payPalConfig;
    private final PayPalAuthService authService;
    private final PayPalCustomerService customerService;
    private final PayPalSubscriptionRepository subscriptionRepository;
    private final RestClient paypalRestClient;
    private final ObjectMapper objectMapper;

    /**
     * Create a PayPal subscription.
     */
    @Transactional
    public PayPalSubscriptionResponse createSubscription(User user, CreateSubscriptionRequest request) {
        log.info("Creating PayPal subscription for user: {}, plan: {}", user.getId(), request.getPlanId());

        try {
            PayPalCustomer customer = customerService.getOrCreateCustomer(user);

            String returnUrl = request.getReturnUrl() != null ? request.getReturnUrl() : payPalConfig.getReturnUrl();
            String cancelUrl = request.getCancelUrl() != null ? request.getCancelUrl() : payPalConfig.getCancelUrl();

            Map<String, Object> subscriptionRequest = Map.of(
                    "plan_id", request.getPlanId(),
                    "quantity", request.getQuantity() != null ? request.getQuantity() : 1,
                    "auto_renewal", request.getAutoRenewal() != null ? request.getAutoRenewal() : true,
                    "application_context", Map.of(
                            "brand_name", "UnravelDocs",
                            "locale", "en-US",
                            "user_action", "SUBSCRIBE_NOW",
                            "return_url", returnUrl,
                            "cancel_url", cancelUrl));

            String response = paypalRestClient.post()
                    .uri("/v1/billing/subscriptions")
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .body(subscriptionRequest)
                    .retrieve()
                    .body(String.class);

            JsonNode subscriptionJson = objectMapper.readTree(response);
            PayPalSubscriptionResponse subResponse = parseSubscriptionResponse(subscriptionJson);

            // Record the subscription
            recordSubscription(user, customer, subResponse, request);

            log.info("Created PayPal subscription: {}", subResponse.getId());
            return subResponse;

        } catch (PayPalPaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create PayPal subscription: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to create subscription", e);
        }
    }

    /**
     * Get subscription details from PayPal.
     */
    public PayPalSubscriptionResponse getSubscriptionDetails(String subscriptionId) {
        log.debug("Getting PayPal subscription details: {}", subscriptionId);

        try {
            String response = paypalRestClient.get()
                    .uri("/v1/billing/subscriptions/{subscriptionId}", subscriptionId)
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .retrieve()
                    .body(String.class);

            JsonNode subscriptionJson = objectMapper.readTree(response);
            return parseSubscriptionResponse(subscriptionJson);

        } catch (Exception e) {
            log.error("Failed to get PayPal subscription details: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to get subscription details", e);
        }
    }

    /**
     * Cancel a PayPal subscription.
     */
    @Transactional
    public PayPalSubscriptionResponse cancelSubscription(String subscriptionId, String reason) {
        log.info("Cancelling PayPal subscription: {}", subscriptionId);

        try {
            Map<String, String> cancelRequest = Map.of(
                    "reason", reason != null ? reason : "Customer requested cancellation");

            paypalRestClient.post()
                    .uri("/v1/billing/subscriptions/{subscriptionId}/cancel", subscriptionId)
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .body(cancelRequest)
                    .retrieve()
                    .toBodilessEntity();

            // Update local record
            updateSubscriptionStatus(subscriptionId, "CANCELLED", reason);

            // Fetch updated subscription details
            return getSubscriptionDetails(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to cancel PayPal subscription: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to cancel subscription", e);
        }
    }

    /**
     * Suspend a PayPal subscription.
     */
    @Transactional
    public PayPalSubscriptionResponse suspendSubscription(String subscriptionId, String reason) {
        log.info("Suspending PayPal subscription: {}", subscriptionId);

        try {
            Map<String, String> suspendRequest = Map.of(
                    "reason", reason != null ? reason : "Subscription suspended");

            paypalRestClient.post()
                    .uri("/v1/billing/subscriptions/{subscriptionId}/suspend", subscriptionId)
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .body(suspendRequest)
                    .retrieve()
                    .toBodilessEntity();

            updateSubscriptionStatus(subscriptionId, "SUSPENDED", reason);

            return getSubscriptionDetails(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to suspend PayPal subscription: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to suspend subscription", e);
        }
    }

    /**
     * Activate/Resume a PayPal subscription.
     */
    @Transactional
    public PayPalSubscriptionResponse activateSubscription(String subscriptionId, String reason) {
        log.info("Activating PayPal subscription: {}", subscriptionId);

        try {
            Map<String, String> activateRequest = Map.of(
                    "reason", reason != null ? reason : "Subscription reactivated");

            paypalRestClient.post()
                    .uri("/v1/billing/subscriptions/{subscriptionId}/activate", subscriptionId)
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .body(activateRequest)
                    .retrieve()
                    .toBodilessEntity();

            updateSubscriptionStatus(subscriptionId, "ACTIVE", reason);

            return getSubscriptionDetails(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to activate PayPal subscription: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to activate subscription", e);
        }
    }

    /**
     * Get subscription by PayPal subscription ID.
     */
    public Optional<PayPalSubscription> getBySubscriptionId(String subscriptionId) {
        return subscriptionRepository.findBySubscriptionId(subscriptionId);
    }

    /**
     * Get active subscription for user.
     */
    public Optional<PayPalSubscription> getActiveSubscriptionByUserId(String userId) {
        return subscriptionRepository.findActiveSubscriptionByUserId(userId);
    }

    /**
     * Get user's subscription history.
     */
    public Page<PayPalSubscription> getSubscriptionsByUserId(String userId, Pageable pageable) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Check if user has an active subscription.
     */
    public boolean hasActiveSubscription(String userId) {
        return subscriptionRepository.findActiveSubscriptionByUserId(userId).isPresent();
    }

    /**
     * Update subscription status.
     */
    @Transactional
    public void updateSubscriptionStatus(String subscriptionId, String status, String reason) {
        subscriptionRepository.findBySubscriptionId(subscriptionId).ifPresent(subscription -> {
            subscription.setStatus(status);
            subscription.setStatusChangeReason(reason);
            if ("CANCELLED".equals(status)) {
                subscription.setCancelledAt(OffsetDateTime.now());
            }
            subscriptionRepository.save(subscription);
            log.info("Updated subscription {} status to {}", subscriptionId, status);
        });
    }

    /**
     * Sync subscription with PayPal (fetch latest state).
     */
    @Transactional
    public PayPalSubscription syncSubscription(String subscriptionId) {
        PayPalSubscription localSub = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new PayPalSubscriptionNotFoundException(subscriptionId, true));

        PayPalSubscriptionResponse paypalSub = getSubscriptionDetails(subscriptionId);

        localSub.setStatus(paypalSub.getStatus());
        if (paypalSub.getBillingInfo() != null) {
            localSub.setNextBillingTime(paypalSub.getBillingInfo().getNextBillingTime());
            localSub.setOutstandingBalance(paypalSub.getBillingInfo().getOutstandingBalance());
            localSub.setCyclesCompleted(paypalSub.getBillingInfo().getCycleExecutionsCount());
            localSub.setFailedPaymentsCount(paypalSub.getBillingInfo().getFailedPaymentsCount());
            localSub.setLastPaymentTime(paypalSub.getBillingInfo().getLastPaymentTime());
            localSub.setLastPaymentAmount(paypalSub.getBillingInfo().getLastPaymentAmount());
        }

        return subscriptionRepository.save(localSub);
    }

    // ==================== Private Helper Methods ====================

    private void recordSubscription(User user, PayPalCustomer customer,
            PayPalSubscriptionResponse response, CreateSubscriptionRequest request) {
        PayPalSubscription subscription = PayPalSubscription.builder()
                .user(user)
                .paypalCustomer(customer)
                .subscriptionId(response.getId())
                .planId(request.getPlanId())
                .status(response.getStatus())
                .customId(request.getCustomId())
                .startTime(response.getStartTime())
                .autoRenewal(request.getAutoRenewal() != null ? request.getAutoRenewal() : true)
                .build();

        subscriptionRepository.save(subscription);
    }

    private PayPalSubscriptionResponse parseSubscriptionResponse(JsonNode subscriptionJson) {
        PayPalSubscriptionResponse.PayPalSubscriptionResponseBuilder builder = PayPalSubscriptionResponse.builder()
                .id(subscriptionJson.get("id").asText())
                .status(subscriptionJson.get("status").asText());

        if (subscriptionJson.has("plan_id")) {
            builder.planId(subscriptionJson.get("plan_id").asText());
        }

        if (subscriptionJson.has("start_time")) {
            builder.startTime(parseDateTime(subscriptionJson.get("start_time").asText()));
        }

        if (subscriptionJson.has("create_time")) {
            builder.createTime(parseDateTime(subscriptionJson.get("create_time").asText()));
        }

        if (subscriptionJson.has("billing_info")) {
            JsonNode billingInfo = subscriptionJson.get("billing_info");
            PayPalSubscriptionResponse.BillingInfo.BillingInfoBuilder billingBuilder = PayPalSubscriptionResponse.BillingInfo
                    .builder();

            if (billingInfo.has("next_billing_time")) {
                billingBuilder.nextBillingTime(parseDateTime(billingInfo.get("next_billing_time").asText()));
            }
            if (billingInfo.has("cycle_executions_count")) {
                billingBuilder.cycleExecutionsCount(billingInfo.get("cycle_executions_count").asInt());
            }
            if (billingInfo.has("failed_payments_count")) {
                billingBuilder.failedPaymentsCount(billingInfo.get("failed_payments_count").asInt());
            }
            if (billingInfo.has("outstanding_balance")) {
                billingBuilder.outstandingBalance(new BigDecimal(
                        billingInfo.get("outstanding_balance").get("value").asText()));
                billingBuilder.currency(billingInfo.get("outstanding_balance").get("currency_code").asText());
            }

            builder.billingInfo(billingBuilder.build());
        }

        if (subscriptionJson.has("links")) {
            List<com.extractor.unraveldocs.payment.paypal.dto.response.PayPalOrderResponse.LinkDescription> links = new java.util.ArrayList<>();
            for (JsonNode linkNode : subscriptionJson.get("links")) {
                links.add(com.extractor.unraveldocs.payment.paypal.dto.response.PayPalOrderResponse.LinkDescription
                        .builder()
                        .href(linkNode.get("href").asText())
                        .rel(linkNode.get("rel").asText())
                        .method(linkNode.has("method") ? linkNode.get("method").asText() : "GET")
                        .build());
            }
            builder.links(links);
        }

        return builder.build();
    }

    private OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null)
            return null;
        try {
            return OffsetDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr);
            return null;
        }
    }
}
