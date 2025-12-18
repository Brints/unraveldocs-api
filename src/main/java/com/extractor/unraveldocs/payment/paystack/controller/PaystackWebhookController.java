package com.extractor.unraveldocs.payment.paystack.controller;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.paystack.dto.webhook.PaystackWebhookEvent;
import com.extractor.unraveldocs.payment.paystack.exception.PaystackWebhookException;
import com.extractor.unraveldocs.payment.paystack.service.PaystackWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling Paystack webhook events
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/paystack/webhook")
@RequiredArgsConstructor
@Tag(name = "Paystack Webhook", description = "Endpoints for Paystack webhook events")
public class PaystackWebhookController {

    private final PaystackWebhookService webhookService;
    private final ObjectMapper objectMapper;
    private SanitizeLogging sanitize;

    /**
     * Handle Paystack webhook events
     * This endpoint should be publicly accessible and configured in Paystack dashboard
     */
    @PostMapping
    @Operation(summary = "Handle webhook events", description = "Receive and process Paystack webhook events")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String payload
    ) {

        try {
            // Parse payload to event object
            PaystackWebhookEvent event = objectMapper.readValue(payload, PaystackWebhookEvent.class);

            log.info("Received Paystack webhook event: {}", sanitize.sanitizeLogging(event.getEvent()));

            // Verify signature
            if (signature != null && !webhookService.verifyWebhookSignature(payload, signature)) {
                log.warn("Invalid webhook signature");
                throw new PaystackWebhookException("Invalid webhook signature");
            }

            // Process the event
            webhookService.processWebhookEvent(event);

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (PaystackWebhookException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process webhook: {}", e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process webhook", e);
        }
    }

    /**
     * Health check for webhook endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Webhook health check", description = "Check if the webhook endpoint is healthy")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Webhook endpoint is healthy");
    }
}
