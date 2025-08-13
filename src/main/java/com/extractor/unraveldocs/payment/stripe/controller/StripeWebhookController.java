package com.extractor.unraveldocs.payment.stripe.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/stripe/webhook")
@Tag(name = "Stripe Webhook", description = "Endpoints for handling Stripe webhooks")
public class StripeWebhookController {
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature verification failed");
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (deserializer.getObject().isPresent()) {
            stripeObject = deserializer.getObject().get();
        } else {
            log.warn("Stripe webhook event data could not be deserialized: {}", event.getType());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Event data deserialization failed");
        }

        // Handle the event based on its type
        switch (event.getType()) {
            case "checkout.session.completed":
                // Handle successful checkout session completion
                Session session = (Session) stripeObject;
                log.info("Checkout session completed: {}", session.getId());

                // TODO: You can add your business logic here, e.g., updating user subscription status
                break;
            case "invoice.payment_succeeded":
                // Handle successful invoice payment
                log.info("Invoice payment succeeded for session: {}", ((Session) stripeObject).getId
                        ());
                break;
            case "invoice.payment_failed":
                // Handle failed invoice payment
                log.warn("Invoice payment failed for session: {}", ((Session) stripeObject).getId());
                break;
            default:
                log.info("Unhandled event type: {}", event.getType());
                return ResponseEntity.status(HttpStatus.OK).body("Event type not handled");
        }
        log.info("Stripe webhook event processed successfully: {}", event.getType());
        return ResponseEntity.status(HttpStatus.OK).body("Webhook event processed successfully");
    }
}
