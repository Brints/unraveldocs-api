package com.extractor.unraveldocs.payment.stripe.adapter;

import com.extractor.unraveldocs.payment.common.dto.*;
import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.common.service.PaymentGatewayService;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.SubscriptionStatus;
import com.extractor.unraveldocs.payment.stripe.dto.request.CreatePaymentIntentRequest;
import com.extractor.unraveldocs.payment.stripe.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.stripe.dto.response.PaymentIntentResponse;
import com.extractor.unraveldocs.payment.stripe.model.StripeCustomer;
import com.extractor.unraveldocs.payment.stripe.service.StripeCustomerService;
import com.extractor.unraveldocs.payment.stripe.service.StripeService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.user.model.User;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe implementation of the unified PaymentGatewayService interface.
 * Adapts existing Stripe services to the common payment abstraction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentGateway implements PaymentGatewayService {

    private final StripeService stripeService;
    private final StripeCustomerService customerService;

    @Override
    public PaymentGateway getProvider() {
        return PaymentGateway.STRIPE;
    }

    @Override
    public InitializePaymentResponse initializeSubscriptionPayment(
            User user,
            SubscriptionPlan plan,
            InitializePaymentRequest request) {
        try {
            Map<String, String> metadata = new HashMap<>();
            if (request.getMetadata() != null) {
                request.getMetadata().forEach((k, v) -> metadata.put(k, String.valueOf(v)));
            }
            metadata.put("plan_id", plan.getId());
            metadata.put("user_id", user.getId());

            Session session = stripeService.createCheckoutSession(
                    user,
                    plan.getStripePriceId(),
                    "subscription",
                    request.getCallbackUrl(),
                    request.getCancelUrl(),
                    1L,
                    null, // Trial days not supported in SubscriptionPlan model yet
                    null,
                    metadata);

            return InitializePaymentResponse.builder()
                    .gateway(PaymentGateway.STRIPE)
                    .paymentUrl(session.getUrl())
                    .reference(session.getId())
                    .accessCode(session.getId())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            return InitializePaymentResponse.builder()
                    .gateway(PaymentGateway.STRIPE)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponse createPayment(User user, PaymentRequest request) {
        try {
            // Use Checkout Sessions for redirect-based flows (e.g., credit purchases)
            if (request.getCallbackUrl() != null && !request.getCallbackUrl().isBlank()) {
                return createCheckoutPayment(user, request);
            }

            // Default: use Payment Intents for client-side confirmation
            CreatePaymentIntentRequest stripeRequest = new CreatePaymentIntentRequest();
            stripeRequest.setAmount(request.getAmount());
            stripeRequest.setCurrency(request.getCurrency());
            stripeRequest.setDescription(request.getDescription());
            stripeRequest.setReceiptEmail(request.getReceiptEmail());
            stripeRequest.setMetadata(request.getMetadata());

            PaymentIntentResponse response = stripeService.createPaymentIntent(user, stripeRequest);

            return PaymentResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .providerPaymentId(response.getId())
                    .status(mapStatus(response.getStatus()))
                    .amount(BigDecimal.valueOf(response.getAmount()).divide(BigDecimal.valueOf(100)))
                    .currency(response.getCurrency())
                    .clientSecret(response.getClientSecret())
                    .createdAt(convertToOffsetDateTime(response.getCreatedAt()))
                    .success(true)
                    .build();

        } catch (StripeException e) {
            return PaymentResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Create a Stripe Checkout Session for redirect-based one-time payments (e.g.,
     * credit packs).
     * Uses price_data so no pre-created Stripe Price is needed.
     */
    private PaymentResponse createCheckoutPayment(User user, PaymentRequest request) throws StripeException {
        StripeCustomer customer = customerService.getOrCreateCustomer(user);

        Map<String, String> metadata = request.getMetadata() != null
                ? new HashMap<>(request.getMetadata())
                : new HashMap<>();

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setCustomer(customer.getStripeCustomerId())
                .setSuccessUrl(request.getCallbackUrl())
                .setCancelUrl(request.getCancelUrl() != null && !request.getCancelUrl().isBlank()
                        ? request.getCancelUrl()
                        : request.getCallbackUrl())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(
                                                        request.getCurrency() != null ? request.getCurrency() : "usd")
                                                .setUnitAmount(request.getAmount())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(request.getDescription() != null
                                                                        ? request.getDescription()
                                                                        : "Credit Pack Purchase")
                                                                .build())
                                                .build())
                                .setQuantity(1L)
                                .build())
                .setMode(SessionCreateParams.Mode.PAYMENT);

        if (!metadata.isEmpty()) {
            paramsBuilder.putAllMetadata(metadata);
        }

        Session session = Session.create(paramsBuilder.build());

        return PaymentResponse.builder()
                .provider(PaymentGateway.STRIPE)
                .providerPaymentId(session.getId())
                .paymentUrl(session.getUrl())
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.valueOf(request.getAmount()).divide(BigDecimal.valueOf(100)))
                .currency(request.getCurrency())
                .success(true)
                .build();
    }

    @Override
    public PaymentResponse getPayment(String providerPaymentId) {
        try {
            PaymentIntentResponse response = stripeService.getPaymentIntent(providerPaymentId);

            return PaymentResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .providerPaymentId(response.getId())
                    .status(mapStatus(response.getStatus()))
                    .amount(BigDecimal.valueOf(response.getAmount()).divide(BigDecimal.valueOf(100)))
                    .currency(response.getCurrency())
                    .description(response.getDescription())
                    .createdAt(convertToOffsetDateTime(response.getCreatedAt()))
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to get Stripe payment: {}", e.getMessage());
            return PaymentResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public RefundResponse refundPayment(RefundRequest request) {
        try {
            com.extractor.unraveldocs.payment.stripe.dto.request.RefundRequest stripeRequest = new com.extractor.unraveldocs.payment.stripe.dto.request.RefundRequest();
            stripeRequest.setPaymentIntentId(request.getPaymentId());
            stripeRequest.setAmount(request.getAmount());
            stripeRequest.setReason(request.getReason());

            var response = stripeService.processRefund(stripeRequest);

            return RefundResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .providerRefundId(response.getId())
                    .paymentId(response.getPaymentIntentId())
                    .status(response.getStatus())
                    .amount(BigDecimal.valueOf(response.getAmount()).divide(BigDecimal.valueOf(100)))
                    .currency(response.getCurrency())
                    .reason(response.getReason())
                    .createdAt(convertToOffsetDateTime(response.getCreatedAt()))
                    .success(true)
                    .build();

        } catch (StripeException e) {
            return RefundResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse createSubscription(User user, SubscriptionRequest request) {
        try {
            CreateSubscriptionRequest stripeRequest = new CreateSubscriptionRequest();
            stripeRequest.setPriceId(request.getPriceId());
            stripeRequest.setQuantity(request.getQuantity());
            stripeRequest.setTrialPeriodDays(request.getTrialPeriodDays());
            stripeRequest.setDefaultPaymentMethodId(request.getPaymentMethodId());
            stripeRequest.setMetadata(request.getMetadata());

            var response = stripeService.createSubscription(user, stripeRequest);

            return mapSubscriptionResponse(response);

        } catch (StripeException e) {
            log.error("Failed to create Stripe subscription: {}", e.getMessage());
            return SubscriptionResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse getSubscription(String providerSubscriptionId) {
        try {
            var response = stripeService.getSubscription(providerSubscriptionId);
            return mapSubscriptionResponse(response);

        } catch (StripeException e) {
            log.error("Failed to get Stripe subscription: {}", e.getMessage());
            return SubscriptionResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse cancelSubscription(String providerSubscriptionId, boolean immediately) {
        try {
            var response = stripeService.cancelSubscription(providerSubscriptionId, immediately);
            return mapSubscriptionResponse(response);

        } catch (StripeException e) {
            log.error("Failed to cancel Stripe subscription: {}", e.getMessage());
            return SubscriptionResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse changePlan(String providerSubscriptionId, String newPriceId) {
        return SubscriptionResponse.builder()
                .provider(PaymentGateway.STRIPE)
                .success(false)
                .errorMessage("Plan change not yet implemented for Stripe")
                .build();
    }

    @Override
    public CustomerResponse getOrCreateCustomer(User user) {
        try {
            StripeCustomer customer = customerService.getOrCreateCustomer(user);

            return CustomerResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .id(customer.getId())
                    .providerCustomerId(customer.getStripeCustomerId())
                    .email(user.getEmail())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .createdAt(customer.getCreatedAt())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to get/create Stripe customer: {}", e.getMessage());
            return CustomerResponse.builder()
                    .provider(PaymentGateway.STRIPE)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean verifyPayment(String reference) {
        try {
            PaymentIntentResponse response = stripeService.getPaymentIntent(reference);
            return "succeeded".equals(response.getStatus());
        } catch (StripeException e) {
            log.error("Failed to verify Stripe payment: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String ensurePlanExists(SubscriptionPlan plan) {
        return plan.getStripePriceId();
    }

    // Helper methods

    private PaymentStatus mapStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.SUCCEEDED;
            case "processing" -> PaymentStatus.PROCESSING;
            case "requires_action" -> PaymentStatus.REQUIRES_ACTION;
            case "requires_payment_method" -> PaymentStatus.REQUIRES_PAYMENT_METHOD;
            case "requires_confirmation" -> PaymentStatus.REQUIRES_CONFIRMATION;
            case "canceled" -> PaymentStatus.CANCELED;
            default -> PaymentStatus.PENDING;
        };
    }

    private SubscriptionResponse mapSubscriptionResponse(
            com.extractor.unraveldocs.payment.stripe.dto.response.SubscriptionResponse response) {
        return SubscriptionResponse.builder()
                .provider(PaymentGateway.STRIPE)
                .providerSubscriptionId(response.getId())
                .status(mapSubscriptionStatus(response.getStatus()))
                .customerId(response.getCustomerId())
                .currentPeriodStart(convertToOffsetDateTime(response.getCurrentPeriodStart()))
                .currentPeriodEnd(convertToOffsetDateTime(response.getCurrentPeriodEnd()))
                .trialEnd(convertToOffsetDateTime(response.getTrialEnd()))
                .cancelAtPeriodEnd(response.isCancelAtPeriodEnd())
                .defaultPaymentMethodId(response.getDefaultPaymentMethodId())
                .latestInvoiceId(response.getLatestInvoiceId())
                .success(true)
                .build();
    }

    private SubscriptionStatus mapSubscriptionStatus(String stripeStatus) {
        return switch (stripeStatus.toLowerCase()) {
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "unpaid" -> SubscriptionStatus.UNPAID;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> SubscriptionStatus.INCOMPLETE_EXPIRED;
            case "paused" -> SubscriptionStatus.PAUSED;
            default -> SubscriptionStatus.ACTIVE;
        };
    }

    private OffsetDateTime convertToOffsetDateTime(Long timestamp) {
        if (timestamp == null)
            return null;
        return Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.UTC);
    }
}
