package com.extractor.unraveldocs.payment.common.events;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Processor for payment events consumed from Kafka.
 * Handles business logic for each event type.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProcessor {

    private final NotificationService notificationService;

    /**
     * Process a payment event.
     * Routes to appropriate handler based on event type.
     */
    public void process(PaymentEvent event) {
        log.debug("Processing payment event: type={}, eventId={}",
                event.getEventType(), event.getEventId());

        switch (event.getEventType()) {
            case PAYMENT_SUCCEEDED -> handlePaymentSucceeded(event);
            case PAYMENT_FAILED -> handlePaymentFailed(event);
            case PAYMENT_CANCELED -> handlePaymentCanceled(event);
            case REFUND_SUCCEEDED, PARTIAL_REFUND_SUCCEEDED -> handleRefund(event);
            case DISPUTE_CREATED -> handleDispute(event);
            default -> log.debug("Unhandled payment event type: {}", event.getEventType());
        }
    }

    /**
     * Process a webhook event (raw provider webhook).
     */
    public void processWebhook(PaymentEvent event) {
        log.debug("Processing webhook event: provider={}, webhookEventId={}",
                event.getProvider(), event.getWebhookEventId());

        // Store raw webhook for audit trail
        // webhookAuditService.store(event);

        // Additional webhook-specific processing
    }

    /**
     * Process a subscription event.
     */
    public void processSubscription(PaymentEvent event) {
        log.debug("Processing subscription event: type={}, subscriptionId={}",
                event.getEventType(), event.getSubscriptionId());

        switch (event.getEventType()) {
            case SUBSCRIPTION_CREATED -> handleSubscriptionCreated(event);
            case SUBSCRIPTION_ACTIVATED -> handleSubscriptionActivated(event);
            case SUBSCRIPTION_RENEWED -> handleSubscriptionRenewed(event);
            case SUBSCRIPTION_CANCELED -> handleSubscriptionCanceled(event);
            case SUBSCRIPTION_EXPIRED -> handleSubscriptionExpired(event);
            case SUBSCRIPTION_PLAN_CHANGED -> handlePlanChanged(event);
            default -> log.debug("Unhandled subscription event type: {}", event.getEventType());
        }
    }

    // ==================== Payment Event Handlers ====================

    private void handlePaymentSucceeded(PaymentEvent event) {
        log.info("Payment succeeded: provider={}, paymentId={}, userId={}, amount={} {}",
                event.getProvider(), event.getProviderPaymentId(),
                event.getUserId(), event.getAmount(), event.getCurrency());

        // Send push notification for successful payment
        sendPaymentNotification(
                event.getUserId(),
                NotificationType.PAYMENT_SUCCESS,
                "Payment Successful",
                String.format("Your payment of %s %s was successful.",
                        event.getCurrency(), event.getAmount()),
                event);
    }

    private void handlePaymentFailed(PaymentEvent event) {
        log.warn("Payment failed: provider={}, paymentId={}, userId={}, error={}",
                event.getProvider(), event.getProviderPaymentId(),
                event.getUserId(), event.getErrorMessage());

        // Send push notification for failed payment
        sendPaymentNotification(
                event.getUserId(),
                NotificationType.PAYMENT_FAILED,
                "Payment Failed",
                event.getErrorMessage() != null
                        ? "Your payment failed: " + event.getErrorMessage()
                        : "Your payment could not be processed. Please try again.",
                event);
    }

    private void handlePaymentCanceled(PaymentEvent event) {
        log.info("Payment canceled: provider={}, paymentId={}, userId={}",
                event.getProvider(), event.getProviderPaymentId(), event.getUserId());

        // TODO: Update payment record, notify user
    }

    private void handleRefund(PaymentEvent event) {
        log.info("Refund processed: provider={}, paymentId={}, userId={}, amount={} {}, partial={}",
                event.getProvider(), event.getProviderPaymentId(), event.getUserId(),
                event.getAmount(), event.getCurrency(),
                event.getEventType() == PaymentEventType.PARTIAL_REFUND_SUCCEEDED);

        // Send push notification for refund
        boolean isPartial = event.getEventType() == PaymentEventType.PARTIAL_REFUND_SUCCEEDED;
        sendPaymentNotification(
                event.getUserId(),
                NotificationType.PAYMENT_REFUNDED,
                isPartial ? "Partial Refund Processed" : "Refund Processed",
                String.format("Your refund of %s %s has been processed.",
                        event.getCurrency(), event.getAmount()),
                event);
    }

    private void handleDispute(PaymentEvent event) {
        log.warn("Dispute created: provider={}, paymentId={}, userId={}, amount={} {}",
                event.getProvider(), event.getProviderPaymentId(),
                event.getUserId(), event.getAmount(), event.getCurrency());

        // TODO: Alert team, freeze account if necessary, prepare evidence
    }

    // ==================== Subscription Event Handlers ====================

    private void handleSubscriptionCreated(PaymentEvent event) {
        log.info("Subscription created: provider={}, subscriptionId={}, userId={}, planId={}",
                event.getProvider(), event.getSubscriptionId(),
                event.getUserId(), event.getPlanId());

        // TODO: Create/update user subscription record
    }

    private void handleSubscriptionActivated(PaymentEvent event) {
        log.info("Subscription activated: subscriptionId={}, userId={}",
                event.getSubscriptionId(), event.getUserId());

        // TODO: Activate user features, send welcome email
    }

    private void handleSubscriptionRenewed(PaymentEvent event) {
        log.info("Subscription renewed: subscriptionId={}, userId={}, amount={} {}",
                event.getSubscriptionId(), event.getUserId(),
                event.getAmount(), event.getCurrency());

        // Send push notification for subscription renewal
        sendPaymentNotification(
                event.getUserId(),
                NotificationType.SUBSCRIPTION_RENEWED,
                "Subscription Renewed",
                String.format("Your subscription has been renewed for %s %s.",
                        event.getCurrency(), event.getAmount()),
                event);
    }

    private void handleSubscriptionCanceled(PaymentEvent event) {
        log.info("Subscription canceled: subscriptionId={}, userId={}",
                event.getSubscriptionId(), event.getUserId());

        // TODO: Schedule feature deactivation, send confirmation
    }

    private void handleSubscriptionExpired(PaymentEvent event) {
        log.info("Subscription expired: subscriptionId={}, userId={}",
                event.getSubscriptionId(), event.getUserId());

        // TODO: Deactivate premium features, notify user
    }

    private void handlePlanChanged(PaymentEvent event) {
        log.info("Subscription plan changed: subscriptionId={}, userId={}, newPlanId={}",
                event.getSubscriptionId(), event.getUserId(), event.getPlanId());

        // TODO: Update user features based on new plan
    }

    /**
     * Helper method to send payment-related push notifications.
     */
    private void sendPaymentNotification(String userId, NotificationType type,
            String title, String message, PaymentEvent event) {
        if (userId == null || userId.isBlank()) {
            log.warn("Cannot send payment notification: userId is null or blank");
            return;
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("provider", event.getProvider() != null ? event.getProvider().name() : "unknown");
            data.put("paymentId", event.getProviderPaymentId());
            if (event.getAmount() != null) {
                data.put("amount", event.getAmount().toString());
            }
            if (event.getCurrency() != null) {
                data.put("currency", event.getCurrency());
            }

            notificationService.sendToUser(userId, type, title, message, data);
            log.debug("Sent {} notification to user {}", type, userId);
        } catch (Exception e) {
            log.error("Failed to send payment notification: {}", e.getMessage());
        }
    }
}
