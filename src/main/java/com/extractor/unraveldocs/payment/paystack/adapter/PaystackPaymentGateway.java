package com.extractor.unraveldocs.payment.paystack.adapter;

import com.extractor.unraveldocs.payment.common.dto.*;
import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.common.service.PaymentGatewayService;
import com.extractor.unraveldocs.payment.enums.SubscriptionStatus;
import com.extractor.unraveldocs.payment.paystack.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.paystack.dto.request.InitializeTransactionRequest;
import com.extractor.unraveldocs.payment.paystack.service.PaystackCustomerService;
import com.extractor.unraveldocs.payment.paystack.service.PaystackPaymentService;
import com.extractor.unraveldocs.payment.paystack.service.PaystackSubscriptionService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Paystack implementation of the unified PaymentGatewayService interface.
 * Adapts existing Paystack services to the common payment abstraction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackPaymentGateway implements PaymentGatewayService {

    private final PaystackPaymentService paymentService;
    private final PaystackCustomerService customerService;
    private final PaystackSubscriptionService subscriptionService;

    @Override
    public PaymentGateway getProvider() {
        return PaymentGateway.PAYSTACK;
    }

    @Override
    public InitializePaymentResponse initializeSubscriptionPayment(
            User user,
            SubscriptionPlan plan,
            InitializePaymentRequest request) {
        try {
            InitializeTransactionRequest paystackRequest = new InitializeTransactionRequest();
            paystackRequest.setEmail(user.getEmail());
            paystackRequest.setAmount(request.getAmountInCents());
            paystackRequest.setCallbackUrl(request.getCallbackUrl());
            paystackRequest.setPlanCode(plan.getPaystackPlanCode());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("plan_id", plan.getId());
            metadata.put("user_id", user.getId());
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }
            paystackRequest.setMetadata(metadata);

            var response = paymentService.initializeTransaction(user, paystackRequest);

            return InitializePaymentResponse.builder()
                    .gateway(PaymentGateway.PAYSTACK)
                    .paymentUrl(response.getAuthorizationUrl())
                    .reference(response.getReference())
                    .accessCode(response.getAccessCode())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to initialize Paystack subscription payment: {}", e.getMessage());
            return InitializePaymentResponse.builder()
                    .gateway(PaymentGateway.PAYSTACK)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponse createPayment(User user, PaymentRequest request) {
        try {
            InitializeTransactionRequest paystackRequest = new InitializeTransactionRequest();
            paystackRequest.setEmail(user.getEmail());
            paystackRequest.setAmount(request.getAmount());

            Map<String, Object> metadata = new HashMap<>();
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }
            paystackRequest.setMetadata(metadata);

            var response = paymentService.initializeTransaction(user, paystackRequest);

            return PaymentResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .providerPaymentId(response.getReference())
                    .status(com.extractor.unraveldocs.payment.enums.PaymentStatus.PENDING)
                    .paymentUrl(response.getAuthorizationUrl())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Paystack payment: {}", e.getMessage());
            return PaymentResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponse getPayment(String providerPaymentId) {
        var payment = paymentService.getPaymentByReference(providerPaymentId);

        if (payment.isPresent()) {
            var p = payment.get();
            return PaymentResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .id(p.getId())
                    .providerPaymentId(p.getReference())
                    .status(p.getStatus())
                    .amount(p.getAmount())
                    .currency(p.getCurrency())
                    .createdAt(p.getCreatedAt())
                    .success(true)
                    .build();
        }

        return PaymentResponse.builder()
                .provider(PaymentGateway.PAYSTACK)
                .success(false)
                .errorMessage("Payment not found")
                .build();
    }

    @Override
    public RefundResponse refundPayment(RefundRequest request) {
        // Paystack refunds are typically done via dashboard or require special API access
        log.warn("Paystack refunds should be processed via the Paystack dashboard");
        return RefundResponse.builder()
                .provider(PaymentGateway.PAYSTACK)
                .success(false)
                .errorMessage("Paystack refunds should be processed via the dashboard")
                .build();
    }

    @Override
    public SubscriptionResponse createSubscription(User user, SubscriptionRequest request) {
        try {
            CreateSubscriptionRequest paystackRequest = CreateSubscriptionRequest.builder()
                    .customer(user.getEmail())
                    .planName(request.getPlanCode())
                    .build();

            var subscription = subscriptionService.createSubscription(user, paystackRequest);

            return SubscriptionResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .providerSubscriptionId(subscription.getSubscriptionCode())
                    .status(mapPaystackStatus(subscription.getStatus()))
                    .currentPeriodEnd(subscription.getNextPaymentDate())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Paystack subscription: {}", e.getMessage());
            return SubscriptionResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse getSubscription(String providerSubscriptionId) {
        var subscription = subscriptionService.getSubscriptionByCode(providerSubscriptionId);

        if (subscription.isPresent()) {
            var s = subscription.get();
            return SubscriptionResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .id(s.getId())
                    .providerSubscriptionId(s.getSubscriptionCode())
                    .status(mapPaystackStatus(s.getStatus()))
                    .currentPeriodStart(s.getCreatedAt())
                    .currentPeriodEnd(s.getNextPaymentDate())
                    .success(true)
                    .build();
        }

        return SubscriptionResponse.builder()
                .provider(PaymentGateway.PAYSTACK)
                .success(false)
                .errorMessage("Subscription not found")
                .build();
    }

    @Override
    public SubscriptionResponse cancelSubscription(String providerSubscriptionId, boolean immediately) {
        try {
            // Paystack requires email token to disable subscription
            var subscription = subscriptionService.getSubscriptionByCode(providerSubscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + providerSubscriptionId));

            subscriptionService.disableSubscription(providerSubscriptionId, subscription.getEmailToken());

            return SubscriptionResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .providerSubscriptionId(providerSubscriptionId)
                    .status(SubscriptionStatus.CANCELED)
                    .cancelAtPeriodEnd(!immediately)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to cancel Paystack subscription: {}", e.getMessage());
            return SubscriptionResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse changePlan(String providerSubscriptionId, String newPriceId) {
        return SubscriptionResponse.builder()
                .provider(PaymentGateway.PAYSTACK)
                .success(false)
                .errorMessage("Plan change not supported for Paystack - cancel and create new subscription")
                .build();
    }

    @Override
    public CustomerResponse getOrCreateCustomer(User user) {
        try {
            var customer = customerService.getOrCreateCustomer(user);

            return CustomerResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .id(customer.getId())
                    .providerCustomerId(customer.getCustomerCode())
                    .email(user.getEmail())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .createdAt(customer.getCreatedAt())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get/create Paystack customer: {}", e.getMessage());
            return CustomerResponse.builder()
                    .provider(PaymentGateway.PAYSTACK)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean verifyPayment(String reference) {
        try {
            var response = paymentService.verifyTransaction(reference);
            return response != null && "success".equalsIgnoreCase(response.getStatus());
        } catch (Exception e) {
            log.error("Failed to verify Paystack payment: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String ensurePlanExists(SubscriptionPlan plan) {
        return plan.getPaystackPlanCode();
    }

    /**
     * Maps Paystack subscription status strings to the unified SubscriptionStatus enum.
     * Paystack uses: active, non-renewing, attention, completed, cancelled
     */
    private SubscriptionStatus mapPaystackStatus(String paystackStatus) {
        if (paystackStatus == null) {
            return SubscriptionStatus.INCOMPLETE;
        }
        return switch (paystackStatus.toLowerCase()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "non-renewing", "completed", "cancelled" -> SubscriptionStatus.CANCELED;
            case "attention" -> SubscriptionStatus.PAST_DUE;
            default -> SubscriptionStatus.INCOMPLETE;
        };
    }
}
