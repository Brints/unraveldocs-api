package com.extractor.unraveldocs.payment.paypal.controller;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.paypal.service.PayPalWebhookService;
import com.extractor.unraveldocs.payment.paypal.service.PayPalWebhookSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    private final PayPalWebhookSignatureService signatureService;
    private final SanitizeLogging sanitizer;

    @Value("${paypal.webhook.verification-enabled:true}")
    private boolean verificationEnabled;

    @PostMapping("/webhook")
    @Operation(summary = "Receive PayPal webhook", description = "Process PayPal webhook events")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-ID", required = false) String transmissionId,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-TIME", required = false) String transmissionTime,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-SIG", required = false) String transmissionSig,
            @RequestHeader(value = "PAYPAL-CERT-URL", required = false) String certUrl,
            @RequestHeader(value = "PAYPAL-AUTH-ALGO", required = false) String authAlgo) {

        log.info("Received PayPal webhook - Transmission ID: {}", sanitizer.sanitizeLogging(transmissionId));

        try {
            // Verify webhook signature if enabled
            if (verificationEnabled) {
                boolean isValid = signatureService.verifyWebhookSignature(
                        transmissionId, transmissionTime, transmissionSig,
                        certUrl, authAlgo, payload);

                if (!isValid) {
                    log.warn("PayPal webhook signature verification failed for transmission ID: {}",
                            sanitizer.sanitizeLogging(transmissionId));
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Webhook signature verification failed");
                }

                log.debug("PayPal webhook signature verified successfully");
            } else {
                log.debug("PayPal webhook signature verification is disabled");
            }

            webhookService.processWebhookEvent(payload);

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (IllegalStateException e) {
            // Webhook ID not configured
            log.error("PayPal webhook configuration error: {}", sanitizer.sanitizeLogging(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook configuration error");
        } catch (Exception e) {
            log.error("Failed to process PayPal webhook: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            // Return 200 to prevent PayPal from retrying
            // Log the error for investigation
            return ResponseEntity.ok("Webhook received but processing failed: " + e.getMessage());
        }
    }
}
