package com.extractor.unraveldocs.payment.paypal.controller;

import com.extractor.unraveldocs.payment.paypal.service.PayPalWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for PayPal webhook events.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/paypal")
@RequiredArgsConstructor
@Tag(name = "PayPal Webhook", description = "Endpoint for receiving PayPal webhook events")
public class PayPalWebhookController {

    private final PayPalWebhookService webhookService;

    @PostMapping("/webhook")
    @Operation(summary = "Receive PayPal webhook", description = "Process PayPal webhook events")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-ID", required = false) String transmissionId,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-TIME", required = false) String transmissionTime,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-SIG", required = false) String transmissionSig,
            @RequestHeader(value = "PAYPAL-CERT-URL", required = false) String certUrl,
            @RequestHeader(value = "PAYPAL-AUTH-ALGO", required = false) String authAlgo) {

        log.info("Received PayPal webhook - Transmission ID: {}", transmissionId);

        try {
            // TODO: Implement webhook signature verification for production
            // For now, we'll process the webhook directly
            // In production, you should verify the signature using PayPal's API

            webhookService.processWebhookEvent(payload);

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Failed to process PayPal webhook: {}", e.getMessage(), e);
            // Return 200 to prevent PayPal from retrying
            // Log the error for investigation
            return ResponseEntity.ok("Webhook received but processing failed: " + e.getMessage());
        }
    }
}
