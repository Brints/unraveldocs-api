package com.extractor.unraveldocs.payment.stripe.service;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.events.ReceiptEventPublisher;
import com.extractor.unraveldocs.payment.stripe.exception.StripePaymentException;
import com.extractor.unraveldocs.payment.stripe.model.StripeCustomer;
import com.extractor.unraveldocs.payment.stripe.model.StripeWebhookEvent;
import com.extractor.unraveldocs.payment.stripe.repository.StripeCustomerRepository;
import com.extractor.unraveldocs.payment.stripe.repository.StripeWebhookEventRepository;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Service for processing Stripe webhook events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final StripeWebhookEventRepository webhookEventRepository;
    private final StripeCustomerRepository customerRepository;
    private final StripePaymentService paymentService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    // Optional dependency - only available when Kafka is configured
    private ReceiptEventPublisher receiptEventPublisher;

    @Autowired(required = false)
    public void setReceiptEventPublisher(ReceiptEventPublisher receiptEventPublisher) {
        this.receiptEventPublisher = receiptEventPublisher;
    }

    /**
     * Check if event has already been processed (idempotency)
     */
    public boolean isEventProcessed(String eventId) {
        return webhookEventRepository.existsByEventId(eventId);
    }

    /**
     * Record webhook event for idempotency
     */
    @Transactional
    public void recordWebhookEvent(String eventId, String eventType, String payload) {
        if (isEventProcessed(eventId)) {
            log.info("Webhook event {} already processed, skipping", eventId);
            webhookEventRepository.findByEventId(eventId).orElseThrow();
            return;
        }

        StripeWebhookEvent event = new StripeWebhookEvent();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setProcessed(false);

        webhookEventRepository.save(event);
    }

    /**
     * Mark event as processed
     */
    @Transactional
    public void markEventAsProcessed(String eventId, String error) {
        webhookEventRepository.findByEventId(eventId).ifPresent(event -> {
            event.setProcessed(true);
            event.setProcessedAt(OffsetDateTime.now());
            if (error != null) {
                event.setProcessingError(error);
            }
            webhookEventRepository.save(event);
        });
    }

    /**
     * Handle checkout session completed event
     */
    @Transactional
    public void handleCheckoutSessionCompleted(Session session) {
        try {
            log.info("Processing checkout.session.completed for session: {}", session.getId());

            String customerId = session.getCustomer();
            String subscriptionId = session.getSubscription();
            String paymentIntentId = session.getPaymentIntent();

            StripeCustomer stripeCustomer = customerRepository.findByStripeCustomerId(customerId)
                    .orElseThrow(() -> new StripePaymentException("Customer not found: " + customerId));

            User user = stripeCustomer.getUser();

            // Determine payment type
            PaymentType paymentType = subscriptionId != null ? PaymentType.SUBSCRIPTION : PaymentType.ONE_TIME;

            // Record the payment
            BigDecimal amount = BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100), 2,
                    RoundingMode.HALF_UP);

            paymentService.recordPayment(
                    user,
                    stripeCustomer,
                    paymentType,
                    paymentIntentId,
                    subscriptionId,
                    null,
                    session.getId(),
                    amount,
                    session.getCurrency(),
                    PaymentStatus.SUCCEEDED,
                    null,
                    "Checkout session completed",
                    null);

            log.info("Successfully processed checkout session completed for user {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to handle checkout session completed: {}", e.getMessage(), e);
            throw new StripePaymentException("Failed to process checkout session", e);
        }
    }

    /**
     * Handle payment intent succeeded event
     */
    @Transactional
    public void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        try {
            log.info("Processing payment_intent.succeeded for: {}", paymentIntent.getId());

            String customerId = paymentIntent.getCustomer();
            StripeCustomer stripeCustomer = customerRepository.findByStripeCustomerId(customerId)
                    .orElseThrow(() -> new StripePaymentException("Customer not found: " + customerId));

            User user = stripeCustomer.getUser();

            // Check if payment already exists
            if (!paymentService.paymentExists(paymentIntent.getId())) {
                BigDecimal amount = BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_UP);

                paymentService.recordPayment(
                        user,
                        stripeCustomer,
                        PaymentType.ONE_TIME,
                        paymentIntent.getId(),
                        null,
                        null,
                        null,
                        amount,
                        paymentIntent.getCurrency(),
                        PaymentStatus.SUCCEEDED,
                        paymentIntent.getPaymentMethod(),
                        paymentIntent.getDescription(),
                        null);
            } else {
                // Update existing payment
                paymentService.updatePaymentStatus(
                        paymentIntent.getId(),
                        PaymentStatus.SUCCEEDED,
                        null,
                        null);
            }

            // Update user subscription from payment metadata (for one-time payments with
            // plan)
            updateUserSubscriptionFromPayment(user, paymentIntent);

            // Generate receipt
            generateReceipt(user, paymentIntent);

            log.info("Successfully processed payment intent succeeded for user {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to handle payment intent succeeded: {}", e.getMessage(), e);
            throw new StripePaymentException("Failed to process payment intent", e);
        }
    }

    /**
     * Handle payment intent payment failed event
     */
    @Transactional
    public void handlePaymentIntentPaymentFailed(PaymentIntent paymentIntent) {
        try {
            log.info("Processing payment_intent.payment_failed for: {}", paymentIntent.getId());

            StripeError lastError = paymentIntent.getLastPaymentError();
            String failureMessage = lastError != null ? lastError.getMessage() : "Payment failed";

            paymentService.updatePaymentStatus(
                    paymentIntent.getId(),
                    PaymentStatus.FAILED,
                    failureMessage,
                    null);

            log.info("Successfully processed payment intent failed for: {}", paymentIntent.getId());
        } catch (Exception e) {
            log.error("Failed to handle payment intent failed: {}", e.getMessage(), e);
            throw new StripePaymentException("Failed to process payment intent failed", e);
        }
    }

    /**
     * Handle customer subscription created event
     */
    @Transactional
    public void handleCustomerSubscriptionCreated(Subscription subscription) {
        try {
            log.info("Processing customer.subscription.created for: {}", subscription.getId());

            String customerId = subscription.getCustomer();
            StripeCustomer stripeCustomer = customerRepository.findByStripeCustomerId(customerId)
                    .orElseThrow(() -> new StripePaymentException("Customer not found: " + customerId));

            User user = stripeCustomer.getUser();

            // Update or create user subscription record
            Optional<UserSubscription> existingSubscription = userSubscriptionRepository.findByUserId(user.getId());

            UserSubscription userSubscription;
            if (existingSubscription.isPresent()) {
                userSubscription = existingSubscription.get();
            } else {
                userSubscription = new UserSubscription();
                userSubscription.setUser(user);
            }

            userSubscription.setPaymentGatewaySubscriptionId(subscription.getId());
            userSubscription.setStatus(subscription.getStatus());

            // Get period info from subscription items
            if (subscription.getItems() != null && subscription.getItems().getData() != null
                    && !subscription.getItems().getData().isEmpty()) {
                SubscriptionItem item = subscription.getItems().getData().getFirst();
                userSubscription.setCurrentPeriodStart(convertToOffsetDateTime(item.getCurrentPeriodStart()));
                userSubscription.setCurrentPeriodEnd(convertToOffsetDateTime(item.getCurrentPeriodEnd()));
            }

            if (subscription.getTrialEnd() != null) {
                userSubscription.setTrialEndsAt(convertToOffsetDateTime(subscription.getTrialEnd()));
            }

            userSubscription.setAutoRenew(!subscription.getCancelAtPeriodEnd());

            userSubscriptionRepository.save(userSubscription);

            log.info("Successfully processed subscription created for user {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to handle subscription created: {}", e.getMessage(), e);
            throw new StripePaymentException("Failed to process subscription created", e);
        }
    }

    /**
     * Handle customer subscription updated event
     */
    @Transactional
    public void handleCustomerSubscriptionUpdated(Subscription subscription) {
        try {
            log.info("Processing customer.subscription.updated for: {}", subscription.getId());

            userSubscriptionRepository.findByPaymentGatewaySubscriptionId(subscription.getId())
                    .ifPresent(userSubscription -> {
                        userSubscription.setStatus(subscription.getStatus());

                        // Get period info from subscription items
                        if (subscription.getItems() != null && subscription.getItems().getData() != null
                                && !subscription.getItems().getData().isEmpty()) {
                            SubscriptionItem item = subscription.getItems().getData().getFirst();
                            userSubscription
                                    .setCurrentPeriodStart(convertToOffsetDateTime(item.getCurrentPeriodStart()));
                            userSubscription.setCurrentPeriodEnd(convertToOffsetDateTime(item.getCurrentPeriodEnd()));
                        }

                        userSubscription.setAutoRenew(!subscription.getCancelAtPeriodEnd());

                        userSubscriptionRepository.save(userSubscription);
                        log.info("Updated subscription {} to status {}", subscription.getId(),
                                subscription.getStatus());
                    });
        } catch (Exception e) {
            log.error("Failed to handle subscription updated: {}", e.getMessage(), e);
            throw new StripePaymentException("Failed to process subscription updated", e);
        }
    }

    /**
     * Handle customer subscription deleted event
     */
    @Transactional
    public void handleCustomerSubscriptionDeleted(Subscription subscription) {
        try {
            log.info("Processing customer.subscription.deleted for: {}", subscription.getId());

            userSubscriptionRepository.findByPaymentGatewaySubscriptionId(subscription.getId())
                    .ifPresent(userSubscription -> {
                        userSubscription.setStatus("CANCELED");
                        userSubscription.setAutoRenew(false);

                        userSubscriptionRepository.save(userSubscription);
                        log.info("Canceled subscription {}", subscription.getId());
                    });
        } catch (Exception e) {
            log.error("Failed to handle subscription deleted: {}", e.getMessage(), e);
            throw new StripePaymentException("Failed to process subscription deleted", e);
        }
    }

    /**
     * Handle invoice payment succeeded event
     */
    @Transactional
    public void handleInvoicePaymentSucceeded(Invoice invoice) {
        try {
            log.info("Processing invoice.payment_succeeded for: {}", invoice.getId());

            String customerId = invoice.getCustomer();
            StripeCustomer stripeCustomer = customerRepository.findByStripeCustomerId(customerId)
                    .orElseThrow(() -> new StripePaymentException("Customer not found: " + customerId));

            User user = stripeCustomer.getUser();

            BigDecimal amount = BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100), 2,
                    RoundingMode.HALF_UP);

            // TODO: Extract payment intent ID and subscription ID
            String paymentIntentId = null;
            String subscriptionId = null;

            paymentService.recordPayment(
                    user,
                    stripeCustomer,
                    PaymentType.SUBSCRIPTION,
                    paymentIntentId,
                    subscriptionId,
                    invoice.getId(),
                    null,
                    amount,
                    invoice.getCurrency(),
                    PaymentStatus.SUCCEEDED,
                    invoice.getDefaultPaymentMethod(),
                    "Invoice payment",
                    null);

            // Generate receipt for subscription payment
            generateReceiptFromInvoice(user, invoice);

            log.info("Successfully processed invoice payment succeeded for user {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to handle invoice payment succeeded: {}", e.getMessage(), e);
            throw new StripePaymentException("Failed to process invoice payment succeeded", e);
        }
    }

    /**
     * Handle invoice payment failed event
     */
    @Transactional
    public void handleInvoicePaymentFailed(Invoice invoice) {
        try {
            log.info("Processing invoice.payment_failed for: {}", invoice.getId());

            // TODO: Update subscription status if needed
            final String subscriptionId = null;

            if (subscriptionId != null) {
                userSubscriptionRepository.findByPaymentGatewaySubscriptionId(subscriptionId)
                        .ifPresent(userSubscription -> {
                            userSubscription.setStatus("PAST_DUE");
                            userSubscriptionRepository.save(userSubscription);
                            log.info("Updated subscription {} to PAST_DUE status", subscriptionId);
                        });
            }

            log.info("Successfully processed invoice payment failed for: {}", invoice.getId());
        } catch (Exception e) {
            log.error("Failed to handle invoice payment failed: {}", e.getMessage(), e);
            throw new StripePaymentException("Failed to process invoice payment failed", e);
        }
    }

    /**
     * Convert Unix timestamp to OffsetDateTime
     */
    private OffsetDateTime convertToOffsetDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC);
    }

    /**
     * Generate receipt for payment intent
     */
    private void generateReceipt(User user, PaymentIntent paymentIntent) {
        try {
            BigDecimal amount = BigDecimal.valueOf(paymentIntent.getAmount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            String paymentMethodDetails = extractPaymentMethodDetails(paymentIntent);

            ReceiptData receiptData = ReceiptData.builder()
                    .userId(user.getId())
                    .customerName(user.getFirstName() + " " + user.getLastName())
                    .customerEmail(user.getEmail())
                    .paymentProvider(PaymentProvider.STRIPE)
                    .externalPaymentId(paymentIntent.getId())
                    .amount(amount)
                    .currency(paymentIntent.getCurrency().toUpperCase())
                    .paymentMethod("card")
                    .paymentMethodDetails(paymentMethodDetails)
                    .description(paymentIntent.getDescription())
                    .paidAt(OffsetDateTime.now())
                    .build();

            if (receiptEventPublisher != null) {
                receiptEventPublisher.publishReceiptRequest(receiptData);
            } else {
                log.debug("Receipt event publisher not available, skipping receipt generation for payment: {}",
                        paymentIntent.getId());
            }
        } catch (Exception e) {
            log.error("Failed to generate receipt for payment {}: {}", paymentIntent.getId(), e.getMessage());
            // Don't rethrow - receipt generation failure shouldn't fail the webhook
        }
    }

    /**
     * Generate receipt for invoice payment
     */
    private void generateReceiptFromInvoice(User user, Invoice invoice) {
        try {
            BigDecimal amount = BigDecimal.valueOf(invoice.getAmountPaid())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            ReceiptData receiptData = ReceiptData.builder()
                    .userId(user.getId())
                    .customerName(user.getFirstName() + " " + user.getLastName())
                    .customerEmail(user.getEmail())
                    .paymentProvider(PaymentProvider.STRIPE)
                    .externalPaymentId(invoice.getId())
                    .amount(amount)
                    .currency(invoice.getCurrency().toUpperCase())
                    .paymentMethod("card")
                    .paymentMethodDetails(null)
                    .description("Subscription Payment")
                    .paidAt(OffsetDateTime.now())
                    .build();

            if (receiptEventPublisher != null) {
                receiptEventPublisher.publishReceiptRequest(receiptData);
            } else {
                log.debug("Receipt event publisher not available, skipping receipt generation for invoice: {}",
                        invoice.getId());
            }
        } catch (Exception e) {
            log.error("Failed to generate receipt for invoice {}: {}", invoice.getId(), e.getMessage());
            // Don't rethrow - receipt generation failure shouldn't fail the webhook
        }
    }

    /**
     * Extract payment method details from payment intent
     */
    private String extractPaymentMethodDetails(PaymentIntent paymentIntent) {
        try {
            if (paymentIntent.getPaymentMethod() != null) {
                // Return placeholder - actual card details would require expanding the payment
                // method
                return "Card ending in ****";
            }
        } catch (Exception e) {
            log.debug("Could not extract payment method details: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Update user subscription from a one-time Stripe payment.
     * Extracts plan info from payment intent metadata and updates the
     * user_subscription table.
     */
    private void updateUserSubscriptionFromPayment(User user, PaymentIntent paymentIntent) {
        try {
            // Extract plan code from metadata
            java.util.Map<String, String> metadata = paymentIntent.getMetadata();
            if (metadata == null || metadata.isEmpty()) {
                log.debug("No metadata in Stripe payment intent {}, skipping subscription update",
                        paymentIntent.getId());
                return;
            }

            String planCode = extractPlanFromMetadata(metadata);
            if (planCode == null || planCode.isBlank()) {
                log.debug("No plan code in Stripe payment metadata for payment {}, skipping subscription update",
                        paymentIntent.getId());
                return;
            }

            log.info("Updating user subscription from Stripe payment {} with plan code: {}",
                    paymentIntent.getId(), planCode);

            // Find the subscription plan
            SubscriptionPlan plan = findSubscriptionPlan(planCode);
            if (plan == null) {
                log.warn("Subscription plan not found for code: {}", planCode);
                return;
            }

            // Get or create user subscription
            UserSubscription userSubscription = userSubscriptionRepository.findByUserId(user.getId())
                    .orElseGet(() -> {
                        UserSubscription newSubscription = new UserSubscription();
                        newSubscription.setUser(user);
                        return newSubscription;
                    });

            // Update subscription details
            OffsetDateTime now = OffsetDateTime.now();
            userSubscription.setPlan(plan);
            userSubscription.setPaymentGatewaySubscriptionId(paymentIntent.getId());
            userSubscription.setStatus("active");
            userSubscription.setCurrentPeriodStart(now);
            userSubscription.setCurrentPeriodEnd(calculatePeriodEnd(plan));
            userSubscription.setAutoRenew(false); // One-time payment, not auto-renewing

            userSubscriptionRepository.save(userSubscription);
            log.info("Successfully updated user subscription for user {} with plan {} via Stripe (period: {} to {})",
                    user.getId(),
                    plan.getName().getPlanName(),
                    userSubscription.getCurrentPeriodStart(),
                    userSubscription.getCurrentPeriodEnd());

        } catch (Exception e) {
            log.error("Failed to update user subscription from Stripe payment {}: {}",
                    paymentIntent.getId(), e.getMessage());
            // Don't rethrow - subscription update failure shouldn't fail the webhook
        }
    }

    /**
     * Extract plan code from payment metadata.
     */
    private String extractPlanFromMetadata(java.util.Map<String, String> metadata) {
        // Try common field names
        String[] fieldNames = { "plan_code", "planCode", "plan", "planId", "plan_id" };
        for (String fieldName : fieldNames) {
            if (metadata.containsKey(fieldName) && metadata.get(fieldName) != null) {
                return metadata.get(fieldName);
            }
        }
        return null;
    }

    /**
     * Find subscription plan by various identifiers.
     */
    private SubscriptionPlan findSubscriptionPlan(String planCode) {
        // Try to match by enum name first
        try {
            SubscriptionPlans planEnum = SubscriptionPlans.fromString(planCode);
            SubscriptionPlan plan = subscriptionPlanRepository.findByName(planEnum).orElse(null);
            if (plan != null) {
                return plan;
            }
        } catch (IllegalArgumentException e) {
            // Not a valid enum name, try other methods
        }

        // Try partial/case-insensitive match
        String normalizedCode = planCode.toUpperCase().replace(" ", "_").replace("-", "_");
        return subscriptionPlanRepository.findAll().stream()
                .filter(plan -> {
                    String planName = plan.getName().name().toUpperCase();
                    return planName.contains(normalizedCode) || normalizedCode.contains(planName);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Calculate the subscription period end based on the plan's billing interval.
     */
    private OffsetDateTime calculatePeriodEnd(SubscriptionPlan plan) {
        OffsetDateTime now = OffsetDateTime.now();
        BillingIntervalUnit intervalUnit = plan.getBillingIntervalUnit();
        int intervalValue = plan.getBillingIntervalValue();

        return switch (intervalUnit) {
            case WEEK -> now.plusWeeks(intervalValue);
            case MONTH -> now.plusMonths(intervalValue);
            case YEAR -> now.plusYears(intervalValue);
        };
    }
}
