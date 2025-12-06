package com.extractor.unraveldocs.payment.stripe.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.stripe.dto.request.CreatePaymentIntentRequest;
import com.extractor.unraveldocs.payment.stripe.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.stripe.dto.request.RefundRequest;
import com.extractor.unraveldocs.payment.stripe.dto.response.PaymentIntentResponse;
import com.extractor.unraveldocs.payment.stripe.dto.response.RefundResponse;
import com.extractor.unraveldocs.payment.stripe.dto.response.SubscriptionResponse;
import com.extractor.unraveldocs.payment.stripe.model.StripeCustomer;
import com.extractor.unraveldocs.user.model.User;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced service for Stripe payment operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeCustomerService customerService;
    private final SanitizeLogging sanitizer;

    @Value("${stripe.checkout.success-url}")
    private String defaultSuccessUrl;

    @Value("${stripe.checkout.cancel-url}")
    private String defaultCancelUrl;

    @Value("${stripe.currency:usd}")
    private String defaultCurrency;

    /**
     * Create a checkout session for subscription or one-time payment
     */
    public Session createCheckoutSession(User user, String priceId, String mode, 
                                         String successUrl, String cancelUrl,
                                         Long quantity, Integer trialPeriodDays,
                                         String promoCode, Map<String, String> metadata) throws StripeException {
        try {
            // Get or create customer
            StripeCustomer customer = customerService.getOrCreateCustomer(user);

            // Build session create params
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setCustomer(customer.getStripeCustomerId())
                    .setSuccessUrl(successUrl != null ? successUrl : defaultSuccessUrl)
                    .setCancelUrl(cancelUrl != null ? cancelUrl : defaultCancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(quantity != null ? quantity : 1L)
                                    .build()
                    )
                    .setMode(SessionCreateParams.Mode.valueOf(mode.toUpperCase()));

            // Add subscription-specific options
            if ("subscription".equalsIgnoreCase(mode)) {
                if (trialPeriodDays != null && trialPeriodDays > 0) {
                    paramsBuilder.setSubscriptionData(
                            SessionCreateParams.SubscriptionData.builder()
                                    .setTrialPeriodDays(trialPeriodDays.longValue())
                                    .build()
                    );
                }
            }

            // Add metadata
            if (metadata != null && !metadata.isEmpty()) {
                paramsBuilder.putAllMetadata(metadata);
            }

            // Add promo code
            if (promoCode != null && !promoCode.isEmpty()) {
                paramsBuilder.addDiscount(
                        SessionCreateParams.Discount.builder()
                                .setPromotionCode(promoCode)
                                .build()
                );
            }

            Session session = Session.create(paramsBuilder.build());
            log.info(
                    "Created checkout session {} for user {}",
                    sanitizer.sanitizeLogging(session.getId()),
                    sanitizer.sanitizeLogging(user.getId()));
            
            return session;
        } catch (StripeException e) {
            log.error(
                    "Failed to create checkout session for user {}: {}",
                    sanitizer.sanitizeLogging(user.getId()),
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Create a payment intent for one-time payment
     */
    public PaymentIntentResponse createPaymentIntent(User user, CreatePaymentIntentRequest request) throws StripeException {
        try {
            StripeCustomer customer = customerService.getOrCreateCustomer(user);

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount())
                    .setCurrency(request.getCurrency() != null ? request.getCurrency() : defaultCurrency)
                    .setCustomer(customer.getStripeCustomerId())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            if (request.getDescription() != null) {
                paramsBuilder.setDescription(request.getDescription());
            }

            if (request.getReceiptEmail() != null) {
                paramsBuilder.setReceiptEmail(request.getReceiptEmail());
            }

            if (request.getMetadata() != null) {
                paramsBuilder.putAllMetadata(request.getMetadata());
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());
            log.info(
                    "Created payment intent {} for user {}",
                    sanitizer.sanitizeLogging(paymentIntent.getId()),
                    sanitizer.sanitizeLogging(user.getId())
            );

            return PaymentIntentResponse.builder()
                    .id(paymentIntent.getId())
                    .status(paymentIntent.getStatus())
                    .amount(paymentIntent.getAmount())
                    .currency(paymentIntent.getCurrency())
                    .clientSecret(paymentIntent.getClientSecret())
                    .createdAt(paymentIntent.getCreated())
                    .build();
        } catch (StripeException e) {
            log.error(
                    "Failed to create payment intent for user {}: {}",
                    sanitizer.sanitizeLogging(user.getId()),
                    e.getMessage());
            throw e;
        }
    }

    /**
     * Create a subscription
     */
    public SubscriptionResponse createSubscription(User user, CreateSubscriptionRequest request) throws StripeException {
        try {
            StripeCustomer customer = customerService.getOrCreateCustomer(user);

            SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                    .setCustomer(customer.getStripeCustomerId())
                    .addItem(
                            SubscriptionCreateParams.Item.builder()
                                    .setPrice(request.getPriceId())
                                    .setQuantity(request.getQuantity())
                                    .build()
                    );

            if (request.getTrialPeriodDays() != null && request.getTrialPeriodDays() > 0) {
                paramsBuilder.setTrialPeriodDays(request.getTrialPeriodDays().longValue());
            }

            if (request.getDefaultPaymentMethodId() != null) {
                paramsBuilder.setDefaultPaymentMethod(request.getDefaultPaymentMethodId());
            }

            if (request.getMetadata() != null) {
                paramsBuilder.putAllMetadata(request.getMetadata());
            }

            Subscription subscription = Subscription.create(paramsBuilder.build());
            log.info(
                    "Created subscription {} for user {}",
                    sanitizer.sanitizeLogging(subscription.getId()),
                    sanitizer.sanitizeLogging(user.getId())
            );

            return mapToSubscriptionResponse(subscription);
        } catch (StripeException e) {
            log.error(
                    "Failed to create subscription for user {}: {}",
                    sanitizer.sanitizeLogging(user.getId()),
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Cancel a subscription
     */
    public SubscriptionResponse cancelSubscription(String subscriptionId, boolean immediately) throws StripeException {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);

            if (immediately) {
                subscription = subscription.cancel();
                log.info(
                        "Immediately canceled subscription {}",
                        sanitizer.sanitizeLogging(subscriptionId)
                );
            } else {
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build();
                subscription = subscription.update(params);
                log.info(
                        "Scheduled subscription {} to cancel at period end",
                        sanitizer.sanitizeLogging(subscriptionId)
                );
            }

            return mapToSubscriptionResponse(subscription);
        } catch (StripeException e) {
            log.error(
                    "Failed to cancel subscription {}: {}",
                    sanitizer.sanitizeLogging(subscriptionId),
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Pause a subscription
     */
    public SubscriptionResponse pauseSubscription(String subscriptionId) throws StripeException {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setPauseCollection(
                            SubscriptionUpdateParams.PauseCollection.builder()
                                    .setBehavior(SubscriptionUpdateParams.PauseCollection.Behavior.VOID)
                                    .build()
                    )
                    .build();

            subscription = subscription.update(params);
            log.info("Paused subscription {}", sanitizer.sanitizeLogging(subscriptionId));

            return mapToSubscriptionResponse(subscription);
        } catch (StripeException e) {
            log.error(
                    "Failed to pause subscription {}: {}",
                    sanitizer.sanitizeLogging(subscriptionId),
                    e.getMessage());
            throw e;
        }
    }

    /**
     * Resume a paused subscription
     */
    public SubscriptionResponse resumeSubscription(String subscriptionId) throws StripeException {
        try {
            //Use Stripe's dedicated Resume endpoint instead of unsetting pause_collection
            // POST /v1/subscriptions/{SUBSCRIPTION_ID}/resume
            // Replace the current implementation with a call to subscription.resume()
            Subscription subscription = Subscription.retrieve(subscriptionId);

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setPauseCollection(SubscriptionUpdateParams.PauseCollection.builder().build())
                    .build();

            subscription = subscription.update(params);
            log.info("Resumed subscription {}", sanitizer.sanitizeLogging(subscriptionId));

            return mapToSubscriptionResponse(subscription);
        } catch (StripeException e) {
            log.error("Failed to resume subscription {}: {}", sanitizer.sanitizeLogging(subscriptionId), e.getMessage());
            throw e;
        }
    }

    /**
     * Process a refund
     */
    public RefundResponse processRefund(RefundRequest request) throws StripeException {
        try {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(request.getPaymentIntentId());

            if (request.getAmount() != null && request.getAmount() > 0) {
                paramsBuilder.setAmount(request.getAmount());
            }

            if (request.getReason() != null) {
                paramsBuilder.setReason(RefundCreateParams.Reason.valueOf(request.getReason().toUpperCase()));
            }

            Refund refund = Refund.create(paramsBuilder.build());
            log.info("Created refund {} for payment intent {}",
                    sanitizer.sanitizeLogging(refund.getId()),
                    sanitizer.sanitizeLogging(refund.getPaymentIntent())
            );

            return RefundResponse.builder()
                    .id(refund.getId())
                    .status(refund.getStatus())
                    .amount(refund.getAmount())
                    .currency(refund.getCurrency())
                    .reason(refund.getReason())
                    .paymentIntentId(refund.getPaymentIntent())
                    .createdAt(refund.getCreated())
                    .build();
        } catch (StripeException e) {
            log.error(
                    "Failed to process refund for payment intent {}: {}",
                    sanitizer.sanitizeLogging(request.getPaymentIntentId()),
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Retrieve payment intent
     */
    public PaymentIntentResponse getPaymentIntent(String paymentIntentId) throws StripeException {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            return PaymentIntentResponse.builder()
                    .id(paymentIntent.getId())
                    .status(paymentIntent.getStatus())
                    .amount(paymentIntent.getAmount())
                    .currency(paymentIntent.getCurrency())
                    .paymentMethodId(paymentIntent.getPaymentMethod())
                    .description(paymentIntent.getDescription())
                    .createdAt(paymentIntent.getCreated())
                    .build();
        } catch (StripeException e) {
            log.error(
                    "Failed to retrieve payment intent {}: {}",
                    sanitizer.sanitizeLogging(paymentIntentId),
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Retrieve subscription
     */
    public SubscriptionResponse getSubscription(String subscriptionId) throws StripeException {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            return mapToSubscriptionResponse(subscription);
        } catch (StripeException e) {
            log.error(
                    "Failed to retrieve subscription {}: {}",
                    sanitizer.sanitizeLogging(subscriptionId),
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Map Stripe Subscription to SubscriptionResponse
     */
    private SubscriptionResponse mapToSubscriptionResponse(Subscription subscription) {
        List<SubscriptionResponse.SubscriptionItem> items = subscription.getItems().getData().stream()
                .map(item -> SubscriptionResponse.SubscriptionItem.builder()
                        .id(item.getId())
                        .priceId(item.getPrice().getId())
                        .quantity(item.getQuantity())
                        .unitAmount(item.getPrice().getUnitAmount())
                        .currency(item.getPrice().getCurrency())
                        .build())
                .collect(Collectors.toList());

        // Get period info from the first subscription item
        Long currentPeriodStart = null;
        Long currentPeriodEnd = null;
        if (subscription.getItems() != null && subscription.getItems().getData() != null && !subscription.getItems().getData().isEmpty()) {
            SubscriptionItem firstItem = subscription.getItems().getData().getFirst();
            currentPeriodStart = firstItem.getCurrentPeriodStart();
            currentPeriodEnd = firstItem.getCurrentPeriodEnd();
        }

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .status(subscription.getStatus())
                .customerId(subscription.getCustomer())
                .currentPeriodStart(currentPeriodStart)
                .currentPeriodEnd(currentPeriodEnd)
                .trialEnd(subscription.getTrialEnd())
                .defaultPaymentMethodId(subscription.getDefaultPaymentMethod())
                .latestInvoiceId(subscription.getLatestInvoice())
                .items(items)
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .build();
    }
}
