package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.paypal.exception.PayPalWebhookException;
import com.extractor.unraveldocs.payment.paypal.model.PayPalWebhookEvent;
import com.extractor.unraveldocs.payment.paypal.repository.PayPalWebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

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
    private final ObjectMapper objectMapper;

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
                log.info("Webhook event {} already processed, skipping", eventId);
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
                    log.info("Unhandled webhook event type: {}", eventType);
            }

            // Mark as processed
            markEventAsProcessed(eventId, null);

        } catch (Exception e) {
            log.error("Failed to process webhook event: {}", e.getMessage(), e);
            throw new PayPalWebhookException("Failed to process webhook event", e);
        }
    }

    // ==================== Payment Event Handlers ====================

    private void handlePaymentCaptureCompleted(JsonNode resource) {
        try {
            String captureId = resource.get("id").asText();
            log.info("Processing PAYMENT.CAPTURE.COMPLETED for: {}", captureId);

            // Find the payment by capture ID and update status
            paymentService.getPaymentByCaptureId(captureId).ifPresent(payment -> {
                paymentService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.SUCCEEDED, null);
            });

        } catch (Exception e) {
            log.error("Failed to handle payment capture completed: {}", e.getMessage(), e);
            throw new PayPalWebhookException("Failed to process payment capture", e);
        }
    }

    private void handlePaymentCaptureDenied(JsonNode resource) {
        try {
            String captureId = resource.get("id").asText();
            log.info("Processing PAYMENT.CAPTURE.DENIED for: {}", captureId);

            paymentService.getPaymentByCaptureId(captureId).ifPresent(payment -> {
                String reason = resource.has("status_details")
                        ? resource.get("status_details").toString()
                        : "Payment denied";
                paymentService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.FAILED, reason);
            });

        } catch (Exception e) {
            log.error("Failed to handle payment capture denied: {}", e.getMessage(), e);
            throw new PayPalWebhookException("Failed to process payment capture denied", e);
        }
    }

    private void handlePaymentCapturePending(JsonNode resource) {
        try {
            String captureId = resource.get("id").asText();
            log.info("Processing PAYMENT.CAPTURE.PENDING for: {}", captureId);

            paymentService.getPaymentByCaptureId(captureId).ifPresent(payment -> {
                paymentService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.PENDING, null);
            });

        } catch (Exception e) {
            log.error("Failed to handle payment capture pending: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentCaptureRefunded(JsonNode resource) {
        try {
            String captureId = resource.get("id").asText();
            log.info("Processing PAYMENT.CAPTURE.REFUNDED for: {}", captureId);

            // The refund is already recorded in refundPayment method
            // This webhook confirms the refund completed

        } catch (Exception e) {
            log.error("Failed to handle payment capture refunded: {}", e.getMessage(), e);
        }
    }

    // ==================== Subscription Event Handlers ====================

    private void handleSubscriptionActivated(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription activated: {}", subscriptionId);

            subscriptionService.updateSubscriptionStatus(subscriptionId, "ACTIVE", "Subscription activated");

        } catch (Exception e) {
            log.error("Failed to handle subscription activated: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionUpdated(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription updated: {}", subscriptionId);

            // Sync with PayPal to get latest state
            subscriptionService.syncSubscription(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to handle subscription updated: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionCancelled(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription cancelled: {}", subscriptionId);

            subscriptionService.updateSubscriptionStatus(subscriptionId, "CANCELLED",
                    "Subscription cancelled via PayPal");

        } catch (Exception e) {
            log.error("Failed to handle subscription cancelled: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionSuspended(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription suspended: {}", subscriptionId);

            subscriptionService.updateSubscriptionStatus(subscriptionId, "SUSPENDED", "Subscription suspended");

        } catch (Exception e) {
            log.error("Failed to handle subscription suspended: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionExpired(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription expired: {}", subscriptionId);

            subscriptionService.updateSubscriptionStatus(subscriptionId, "EXPIRED", "Subscription expired");

        } catch (Exception e) {
            log.error("Failed to handle subscription expired: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionPaymentFailed(JsonNode resource) {
        try {
            String subscriptionId = resource.get("id").asText();
            log.info("Processing subscription payment failed: {}", subscriptionId);

            // Sync to update failed payment count
            subscriptionService.syncSubscription(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to handle subscription payment failed: {}", e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

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
}
