package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.service.CouponValidationService;
import com.extractor.unraveldocs.credit.service.CreditPurchaseService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.paypal.exception.PayPalWebhookException;
import com.extractor.unraveldocs.payment.paypal.model.PayPalPayment;
import com.extractor.unraveldocs.payment.paypal.model.PayPalSubscription;
import com.extractor.unraveldocs.payment.paypal.model.PayPalWebhookEvent;
import com.extractor.unraveldocs.payment.paypal.repository.PayPalSubscriptionRepository;
import com.extractor.unraveldocs.payment.paypal.repository.PayPalWebhookEventRepository;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Service for processing PayPal webhook events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalWebhookService {

    private final PayPalPaymentService paymentService;
    private final PayPalSubscriptionService subscriptionService;
    private final PayPalWebhookEventRepository webhookEventRepository;
    private final PayPalSubscriptionRepository paypalSubscriptionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final CouponValidationService couponValidationService;
    private final CouponRepository couponRepository;
    private final CreditPurchaseService creditPurchaseService;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitizer;

    // ==================== Payment Event Types ====================
    private static final String PAYMENT_CAPTURE_COMPLETED = "PAYMENT.CAPTURE.COMPLETED";
    private static final String PAYMENT_CAPTURE_DENIED = "PAYMENT.CAPTURE.DENIED";
    private static final String PAYMENT_CAPTURE_PENDING = "PAYMENT.CAPTURE.PENDING";
    private static final String PAYMENT_CAPTURE_REFUNDED = "PAYMENT.CAPTURE.REFUNDED";

    // ==================== Subscription Event Types ====================
    private static final String BILLING_SUBSCRIPTION_CREATED = "BILLING.SUBSCRIPTION.CREATED";
    private static final String BILLING_SUBSCRIPTION_ACTIVATED = "BILLING.SUBSCRIPTION.ACTIVATED";
    private static final String BILLING_SUBSCRIPTION_UPDATED = "BILLING.SUBSCRIPTION.UPDATED";
    private static final String BILLING_SUBSCRIPTION_CANCELLED = "BILLING.SUBSCRIPTION.CANCELLED";
    private static final String BILLING_SUBSCRIPTION_SUSPENDED = "BILLING.SUBSCRIPTION.SUSPENDED";
    private static final String BILLING_SUBSCRIPTION_EXPIRED = "BILLING.SUBSCRIPTION.EXPIRED";
    private static final String BILLING_SUBSCRIPTION_PAYMENT_FAILED = "BILLING.SUBSCRIPTION.PAYMENT.FAILED";

    /**
     * Check if event has already been processed (idempotency).
     */
    public boolean isEventProcessed(String eventId) {
        return webhookEventRepository.existsByEventId(eventId);
    }

    /**
     * Process a webhook event.
     */
    @Transactional
    public void processWebhookEvent(String payload) {
        try {
            JsonNode eventJson = objectMapper.readTree(payload);

            String eventId = eventJson.get("id").asText();
            String eventType = eventJson.get("event_type").asText();

            // Check for duplicate processing (idempotency)
            if (isEventProcessed(eventId)) {
                log.info("Webhook event {} already processed, skipping", sanitizer.sanitizeLogging(eventId));
                return;
            }

            // Record the event
            recordWebhookEvent(eventId, eventType, payload, eventJson);

            // Process based on event type
            JsonNode resource = eventJson.get("resource");

            switch (eventType) {
                // Payment events
                case PAYMENT_CAPTURE_COMPLETED:
                    handlePaymentCaptureCompleted(resource);
                    break;
                case PAYMENT_CAPTURE_DENIED:
                    handlePaymentCaptureDenied(resource);
                    break;
                case PAYMENT_CAPTURE_PENDING:
                    handlePaymentCapturePending(resource);
                    break;
                case PAYMENT_CAPTURE_REFUNDED:
                    handlePaymentCaptureRefunded(resource);
                    break;

                // Subscription events
                case BILLING_SUBSCRIPTION_CREATED:
                case BILLING_SUBSCRIPTION_ACTIVATED:
                    handleSubscriptionActivated(resource);
                    break;
                case BILLING_SUBSCRIPTION_UPDATED:
                    handleSubscriptionUpdated(resource);
                    break;
                case BILLING_SUBSCRIPTION_CANCELLED:
                    handleSubscriptionCancelled(resource);
                    break;
                case BILLING_SUBSCRIPTION_SUSPENDED:
                    handleSubscriptionSuspended(resource);
                    break;
                case BILLING_SUBSCRIPTION_EXPIRED:
                    handleSubscriptionExpired(resource);
                    break;
                case BILLING_SUBSCRIPTION_PAYMENT_FAILED:
                    handleSubscriptionPaymentFailed(resource);
                    break;

                default:
                    log.info("Unhandled webhook event type: {}", sanitizer.sanitizeLogging(eventType));
            }

            // Mark as processed
            markEventAsProcessed(eventId, null);

        } catch (Exception e) {
            log.error("Failed to process webhook event: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            throw new PayPalWebhookException("Failed to process webhook event", e);
        }
    }

    // ==================== Payment Event Handlers ====================

    private void handlePaymentCaptureCompleted(JsonNode resource) {
        try {
            String captureId = resource.get("id").asText();
            log.info("Processing PAYMENT.CAPTURE.COMPLETED for: {}", sanitizer.sanitizeLogging(captureId));

            // Find the payment by capture ID and update status
            paymentService.getPaymentByCaptureId(captureId).ifPresent(payment -> {
                paymentService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.SUCCEEDED, null);

                // Record coupon usage if applicable
                recordCouponUsageIfApplicable(payment);

                // Complete credit pack purchase or update user subscription
                if (payment.getPaymentType() == PaymentType.CREDIT_PURCHASE) {
                    completeCreditPurchase(payment);
                } else {
                    // Update user subscription from payment (for one-time payments with plan)
                    updateUserSubscriptionFromPayment(payment);
                }
            });

        } catch (Exception e) {
            log.error("Failed to handle payment capture completed: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            throw new PayPalWebhookException("Failed to process payment capture", e);
        }
    }

    private void handlePaymentCaptureDenied(JsonNode resource) {
        try {
            String captureId = resource.get("id").asText();
            log.info("Processing PAYMENT.CAPTURE.DENIED for: {}", sanitizer.sanitizeLogging(captureId));

            paymentService.getPaymentByCaptureId(captureId).ifPresent(payment -> {
                String reason = resource.has("status_details")
                        ? resource.get("status_details").toString()
                        : "Payment denied";
                paymentService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.FAILED, reason);
            });

        } catch (Exception e) {
            log.error("Failed to handle payment capture denied: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            throw new PayPalWebhookException("Failed to process payment capture denied", e);
        }
    }

    private void handlePaymentCapturePending(JsonNode resource) {
        try {
            String captureId = resource.get("id").asText();
            log.info("Processing PAYMENT.CAPTURE.PENDING for: {}", sanitizer.sanitizeLogging(captureId));

            paymentService.getPaymentByCaptureId(captureId).ifPresent(payment -> {
                paymentService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.PENDING, null);
            });

        } catch (Exception e) {
            log.error("Failed to handle payment capture pending: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
        }
    }

    private void handlePaymentCaptureRefunded(JsonNode resource) {
        try {
            String captureId = resource.get("id").asText();
            log.info("Processing PAYMENT.CAPTURE.REFUNDED for: {}", sanitizer.sanitizeLogging(captureId));

            // The refund is already recorded in refundPayment method
            // This webhook confirms the refund completed

        } catch (Exception e) {
            log.error("Failed to handle payment capture refunded: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
        }
    }

    // ==================== Subscription Event Handlers ====================

    private void handleSubscriptionActivated(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription activated: {}", sanitizer.sanitizeLogging(subscriptionId));

            subscriptionService.updateSubscriptionStatus(subscriptionId, "ACTIVE", "Subscription activated");

            // Update user subscription table
            updateUserSubscriptionFromPayPalSubscription(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to handle subscription activated: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
        }
    }

    private void handleSubscriptionUpdated(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription updated: {}", sanitizer.sanitizeLogging(subscriptionId));

            // Sync with PayPal to get latest state
            subscriptionService.syncSubscription(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to handle subscription updated: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
        }
    }

    private void handleSubscriptionCancelled(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription cancelled: {}", sanitizer.sanitizeLogging(subscriptionId));

            subscriptionService.updateSubscriptionStatus(subscriptionId, "CANCELLED",
                    "Subscription cancelled via PayPal");

        } catch (Exception e) {
            log.error("Failed to handle subscription cancelled: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionSuspended(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription suspended: {}", sanitizer.sanitizeLogging(subscriptionId));

            subscriptionService.updateSubscriptionStatus(subscriptionId, "SUSPENDED", "Subscription suspended");

        } catch (Exception e) {
            log.error("Failed to handle subscription suspended: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
        }
    }

    private void handleSubscriptionExpired(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription expired: {}", sanitizer.sanitizeLogging(subscriptionId));

            subscriptionService.updateSubscriptionStatus(subscriptionId, "EXPIRED", "Subscription expired");

        } catch (Exception e) {
            log.error("Failed to handle subscription expired: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
        }
    }

    private void handleSubscriptionPaymentFailed(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription payment failed: {}", sanitizer.sanitizeLogging(subscriptionId));

            // Sync to update failed payment count
            subscriptionService.syncSubscription(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to handle subscription payment failed: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Update user subscription from PayPal subscription data after activation.
     * This ensures the user_subscription table is updated when a subscription is
     * activated.
     */
    private void updateUserSubscriptionFromPayPalSubscription(String paypalSubscriptionId) {
        try {
            PayPalSubscription paypalSubscription = paypalSubscriptionRepository
                    .findBySubscriptionId(paypalSubscriptionId)
                    .orElse(null);

            if (paypalSubscription == null) {
                log.warn("PayPal subscription not found for ID: {}", sanitizer.sanitizeLogging(paypalSubscriptionId));
                return;
            }

            User user = paypalSubscription.getUser();
            String planId = paypalSubscription.getPlanId();

            // Try to find the subscription plan
            SubscriptionPlan plan = findSubscriptionPlanByPayPalPlanId(planId);
            if (plan == null) {
                log.warn("Subscription plan not found for PayPal plan ID: {}", sanitizer.sanitizeLogging(planId));
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
            userSubscription.setPlan(plan);
            userSubscription.setPaymentGatewaySubscriptionId(paypalSubscriptionId);
            userSubscription.setStatus("active");
            userSubscription.setCurrentPeriodStart(paypalSubscription.getStartTime() != null
                    ? paypalSubscription.getStartTime()
                    : OffsetDateTime.now());

            // Calculate period end based on billing interval or use next billing time
            if (paypalSubscription.getNextBillingTime() != null) {
                userSubscription.setCurrentPeriodEnd(paypalSubscription.getNextBillingTime());
            } else {
                userSubscription.setCurrentPeriodEnd(calculatePeriodEnd(plan));
            }

            userSubscription.setAutoRenew(Boolean.TRUE.equals(paypalSubscription.getAutoRenewal()));

            userSubscriptionRepository.save(userSubscription);
            log.info("Updated user subscription for user {} with plan {} via PayPal",
                    sanitizer.sanitizeLogging(user.getId()),
                    sanitizer.sanitizeLogging(plan.getName().getPlanName()));

        } catch (Exception e) {
            log.error("Failed to update user subscription for PayPal subscription {}: {}",
                    sanitizer.sanitizeLogging(paypalSubscriptionId), e.getMessage());
            // Don't rethrow - user subscription update failure shouldn't fail the webhook
        }
    }

    /**
     * Update user subscription from a one-time PayPal payment.
     * Extracts plan info from payment metadata and updates the user_subscription
     * table.
     */
    private void updateUserSubscriptionFromPayment(PayPalPayment payment) {
        try {
            User user = payment.getUser();

            // Extract plan code from metadata
            String planCode = extractPlanFromMetadata(payment.getMetadata());
            if (planCode == null || planCode.isBlank()) {
                log.debug("No plan code in PayPal payment metadata for order {}, skipping subscription update",
                        sanitizer.sanitizeLogging(payment.getOrderId()));
                return;
            }

            log.info("Updating user subscription from PayPal payment {} with plan code: {}",
                    sanitizer.sanitizeLogging(payment.getOrderId()),
                    sanitizer.sanitizeLogging(planCode));

            // Find the subscription plan
            SubscriptionPlan plan = findSubscriptionPlan(planCode);
            if (plan == null) {
                log.warn("Subscription plan not found for code: {}", sanitizer.sanitizeLogging(planCode));
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
            userSubscription.setPaymentGatewaySubscriptionId(payment.getOrderId());
            userSubscription.setStatus("active");
            userSubscription.setCurrentPeriodStart(now);
            userSubscription.setCurrentPeriodEnd(calculatePeriodEnd(plan));
            userSubscription.setAutoRenew(false); // One-time payment, not auto-renewing

            userSubscriptionRepository.save(userSubscription);
            log.info("Successfully updated user subscription for user {} with plan {} via PayPal (period: {} to {})",
                    sanitizer.sanitizeLogging(user.getId()),
                    sanitizer.sanitizeLogging(plan.getName().getPlanName()),
                    userSubscription.getCurrentPeriodStart(),
                    userSubscription.getCurrentPeriodEnd());

        } catch (Exception e) {
            log.error("Failed to update user subscription from PayPal payment {}: {}",
                    sanitizer.sanitizeLogging(payment.getOrderId()), e.getMessage());
            // Don't rethrow - subscription update failure shouldn't fail the webhook
        }
    }

    /**
     * Extract plan code from payment metadata.
     */
    private String extractPlanFromMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode metadata = objectMapper.readTree(metadataJson);
            // Try common field names
            String[] fieldNames = { "plan_code", "planCode", "plan", "planId", "plan_id" };
            for (String fieldName : fieldNames) {
                if (metadata.has(fieldName) && !metadata.get(fieldName).isNull()) {
                    return metadata.get(fieldName).asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse payment metadata: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Find subscription plan by various identifiers.
     */
    private SubscriptionPlan findSubscriptionPlan(String planCode) {
        // Try to match by enum name first
        try {
            com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans planEnum = com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans
                    .fromString(planCode);
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
     * Find subscription plan by PayPal plan ID.
     */
    private SubscriptionPlan findSubscriptionPlanByPayPalPlanId(String paypalPlanId) {
        // Try to find by PayPal plan code
        return subscriptionPlanRepository.findAll().stream()
                .filter(plan -> paypalPlanId.equals(plan.getPaypalPlanCode()))
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

    private void recordWebhookEvent(String eventId, String eventType, String payload, JsonNode eventJson) {
        PayPalWebhookEvent event = PayPalWebhookEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .payload(payload)
                .processed(false)
                .build();

        if (eventJson.has("resource_type")) {
            event.setResourceType(eventJson.get("resource_type").asText());
        }

        if (eventJson.has("resource") && eventJson.get("resource").has("id")) {
            event.setResourceId(eventJson.get("resource").get("id").asText());
        }

        webhookEventRepository.save(event);
    }

    private void markEventAsProcessed(String eventId, String error) {
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
     * Records coupon usage if the payment used a coupon.
     */
    private void recordCouponUsageIfApplicable(PayPalPayment payment) {
        if (payment.getCouponCode() == null || payment.getCouponCode().isBlank()) {
            return;
        }

        try {
            Coupon coupon = couponRepository.findByCode(payment.getCouponCode().toUpperCase()).orElse(null);
            if (coupon == null) {
                log.warn("Coupon not found for code: {} during usage recording",
                        sanitizer.sanitizeLogging(payment.getCouponCode()));
                return;
            }

            couponValidationService.recordCouponUsage(
                    coupon,
                    payment.getUser(),
                    payment.getOriginalAmount(),
                    payment.getAmount(),
                    payment.getOrderId(),
                    null // subscriptionPlan can be null for one-time payments
            );

            log.info("Recorded coupon usage for code: {} on PayPal payment: {}",
                    sanitizer.sanitizeLogging(payment.getCouponCode()),
                    sanitizer.sanitizeLogging(payment.getOrderId()));

        } catch (Exception e) {
            log.error("Failed to record coupon usage for payment {}: {}",
                    sanitizer.sanitizeLogging(payment.getOrderId()),
                    sanitizer.sanitizeLogging(e.getMessage()), e);
            // Don't throw - coupon usage recording should not fail the webhook
        }
    }

    /**
     * Complete credit pack purchase after payment confirmation.
     * Extracts creditPackId from payment metadata and delegates to
     * CreditPurchaseService.
     */
    private void completeCreditPurchase(PayPalPayment payment) {
        try {
            String metadataJson = payment.getMetadata();
            if (metadataJson == null || metadataJson.isBlank()) {
                log.error("No metadata found for credit purchase payment: {}",
                        sanitizer.sanitizeLogging(payment.getOrderId()));
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
            String creditPackId = metadata.get("creditPackId") != null
                    ? String.valueOf(metadata.get("creditPackId"))
                    : null;

            if (creditPackId == null || creditPackId.isBlank()) {
                log.error("No creditPackId in metadata for credit purchase payment: {}",
                        sanitizer.sanitizeLogging(payment.getOrderId()));
                return;
            }

            User user = payment.getUser();
            log.info("Completing credit pack purchase for user {}, pack {}, payment {}",
                    sanitizer.sanitizeLogging(user.getId()),
                    sanitizer.sanitizeLogging(creditPackId),
                    sanitizer.sanitizeLogging(payment.getOrderId()));

            creditPurchaseService.completePurchase(user, creditPackId, payment.getOrderId());

            log.info("Successfully completed credit pack purchase for user {}",
                    sanitizer.sanitizeLogging(user.getId()));

        } catch (Exception e) {
            log.error("Failed to complete credit pack purchase for payment {}: {}",
                    sanitizer.sanitizeLogging(payment.getOrderId()), e.getMessage(), e);
            // Don't rethrow - credit fulfillment failure shouldn't fail the entire webhook
        }
    }
}
