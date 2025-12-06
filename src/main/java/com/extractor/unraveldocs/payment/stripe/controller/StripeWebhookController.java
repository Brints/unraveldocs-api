package com.extractor.unraveldocs.payment.stripe.controller;

import com.extractor.unraveldocs.payment.stripe.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling Stripe webhooks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stripe/webhook")
@RequiredArgsConstructor
@Tag(name = "Stripe Webhook", description = "Endpoints for handling Stripe webhooks")
public class StripeWebhookController {
    
    private final StripeWebhookService webhookService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<@NonNull String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        Event event;

        // Verify webhook signature
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature verification failed");
        }

        // Check if event was already processed (idempotency)
        if (webhookService.isEventProcessed(event.getId())) {
            log.info("Webhook event {} already processed", event.getId());
            return ResponseEntity.ok("Event already processed");
        }

        // Record the event
        webhookService.recordWebhookEvent(event.getId(), event.getType(), payload);

        try {
            // Get the data object
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject;
            
            if (deserializer.getObject().isPresent()) {
                stripeObject = deserializer.getObject().get();
            } else {
                log.warn("Stripe webhook event data could not be deserialized: {}", event.getType());
                webhookService.markEventAsProcessed(event.getId(), "Deserialization failed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Event data deserialization failed");
            }

            // Handle different event types
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
                    log.info("Unhandled event type: {}", event.getType());
                    webhookService.markEventAsProcessed(event.getId(), null);
                    return ResponseEntity.ok("Event type not handled");
            }

            // Mark event as successfully processed
            webhookService.markEventAsProcessed(event.getId(), null);
            log.info("Successfully processed webhook event: {}", event.getType());
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing webhook event {}: {}", event.getType(), e.getMessage(), e);
            webhookService.markEventAsProcessed(event.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
}
