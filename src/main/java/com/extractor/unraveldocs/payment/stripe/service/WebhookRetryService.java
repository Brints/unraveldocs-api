package com.extractor.unraveldocs.payment.stripe.service;

import com.extractor.unraveldocs.payment.stripe.model.StripeWebhookEvent;
import com.extractor.unraveldocs.payment.stripe.repository.StripeWebhookEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service for retrying failed webhook events with exponential backoff.
 * Failed events are retried up to MAX_RETRIES times before being marked
 * as permanently failed (dead letter).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRetryService {

    private static final int MAX_RETRIES = 3;
    private static final int[] RETRY_DELAYS_MINUTES = {5, 15, 60}; // Exponential backoff

    private final StripeWebhookEventRepository webhookEventRepository;
    private final StripeWebhookService webhookService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * Scheduled job to retry failed webhook events.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void retryFailedWebhooks() {
        List<StripeWebhookEvent> eventsToRetry = webhookEventRepository
                .findEventsToRetry(OffsetDateTime.now());

        if (eventsToRetry.isEmpty()) {
            return;
        }

        log.info("Found {} webhook events to retry", eventsToRetry.size());

        for (StripeWebhookEvent event : eventsToRetry) {
            retryEvent(event);
        }
    }

    /**
     * Retry a single webhook event
     */
    @Transactional
    public void retryEvent(StripeWebhookEvent event) {
        log.info("Retrying webhook event {} (attempt {})", event.getEventId(), event.getRetryCount() + 1);

        try {
            // Deserialize the stored payload directly since signature was verified on initial receipt
            Event stripeEvent = Event.GSON.fromJson(event.getPayload(), Event.class);
            processEvent(stripeEvent, event);

            // Mark as successfully processed
            event.setProcessed(true);
            event.setProcessedAt(OffsetDateTime.now());
            event.setProcessingError(null);
            webhookEventRepository.save(event);

            log.info("Successfully processed webhook event {} on retry", event.getEventId());

        } catch (SignatureVerificationException e) {
            // This shouldn't happen since we already verified the signature
            log.error("Signature verification failed on retry for event {}: {}",
                    event.getEventId(), e.getMessage());
            markEventFailed(event, "Signature verification failed: " + e.getMessage());

        } catch (Exception e) {
            log.error("Failed to process webhook event {} on retry: {}",
                    event.getEventId(), e.getMessage(), e);
            handleRetryFailure(event, e.getMessage());
        }
    }

    /**
     * Process the webhook event based on its type
     */
    private void processEvent(Event event, StripeWebhookEvent webhookEvent) throws Exception {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isEmpty()) {
            throw new RuntimeException("Failed to deserialize event data");
        }

        StripeObject stripeObject = deserializer.getObject().get();

        switch (event.getType()) {
            case "checkout.session.completed":
                webhookService.handleCheckoutSessionCompleted((Session) stripeObject);
                break;

            case "payment_intent.succeeded":
                webhookService.handlePaymentIntentSucceeded((PaymentIntent) stripeObject);
                break;

            case "payment_intent.payment_failed":
                webhookService.handlePaymentIntentPaymentFailed((PaymentIntent) stripeObject);
                break;

            case "customer.subscription.created":
                webhookService.handleCustomerSubscriptionCreated((Subscription) stripeObject);
                break;

            case "customer.subscription.updated":
                webhookService.handleCustomerSubscriptionUpdated((Subscription) stripeObject);
                break;

            case "customer.subscription.deleted":
                webhookService.handleCustomerSubscriptionDeleted((Subscription) stripeObject);
                break;

            case "invoice.payment_succeeded":
                webhookService.handleInvoicePaymentSucceeded((Invoice) stripeObject);
                break;

            case "invoice.payment_failed":
                webhookService.handleInvoicePaymentFailed((Invoice) stripeObject);
                break;

            default:
                log.info("Unhandled event type on retry: {}", event.getType());
        }
    }

    /**
     * Handle retry failure - increment counter and schedule next retry or mark as dead letter
     */
    private void handleRetryFailure(StripeWebhookEvent event, String errorMessage) {
        int newRetryCount = event.getRetryCount() + 1;
        event.setRetryCount(newRetryCount);
        event.setProcessingError(errorMessage);

        if (newRetryCount >= MAX_RETRIES) {
            // Max retries reached - move to dead letter
            event.setMaxRetriesReached(true);
            event.setNextRetryAt(null);
            log.warn("Webhook event {} has reached max retries ({}), moving to dead letter",
                    event.getEventId(), MAX_RETRIES);
        } else {
            // Schedule next retry with exponential backoff
            int delayMinutes = RETRY_DELAYS_MINUTES[Math.min(newRetryCount - 1, RETRY_DELAYS_MINUTES.length - 1)];
            event.setNextRetryAt(OffsetDateTime.now().plusMinutes(delayMinutes));
            log.info("Scheduled retry for webhook event {} in {} minutes",
                    event.getEventId(), delayMinutes);
        }

        webhookEventRepository.save(event);
    }

    /**
     * Mark event as permanently failed
     */
    private void markEventFailed(StripeWebhookEvent event, String errorMessage) {
        event.setProcessingError(errorMessage);
        event.setMaxRetriesReached(true);
        event.setNextRetryAt(null);
        webhookEventRepository.save(event);
    }

    /**
     * Get count of events in dead letter (max retries reached)
     */
    public long getDeadLetterCount() {
        return webhookEventRepository.countByMaxRetriesReachedTrue();
    }

    /**
     * Get dead letter events for manual review
     */
    public List<StripeWebhookEvent> getDeadLetterEvents() {
        return webhookEventRepository.findByMaxRetriesReachedTrue();
    }

    /**
     * Manually retry a dead letter event (resets retry count)
     */
    @Transactional
    public void manualRetry(String eventId) {
        webhookEventRepository.findByEventId(eventId).ifPresent(event -> {
            event.setRetryCount(0);
            event.setMaxRetriesReached(false);
            event.setNextRetryAt(OffsetDateTime.now());
            webhookEventRepository.save(event);
            log.info("Manually scheduled retry for dead letter event {}", eventId);
        });
    }
}
