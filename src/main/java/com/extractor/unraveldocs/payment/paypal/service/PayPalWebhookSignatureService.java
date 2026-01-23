package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.payment.paypal.config.PayPalConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service for verifying PayPal webhook signatures.
 * Uses PayPal's verify-webhook-signature API endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalWebhookSignatureService {

    private final PayPalConfig payPalConfig;
    private final PayPalAuthService authService;
    private final RestClient paypalRestClient;
    private final ObjectMapper objectMapper;

    private static final String VERIFICATION_STATUS_SUCCESS = "SUCCESS";

    /**
     * Verify webhook signature by calling PayPal's verify-webhook-signature API.
     *
     * @param transmissionId   The PAYPAL-TRANSMISSION-ID header value
     * @param transmissionTime The PAYPAL-TRANSMISSION-TIME header value
     * @param transmissionSig  The PAYPAL-TRANSMISSION-SIG header value
     * @param certUrl          The PAYPAL-CERT-URL header value
     * @param authAlgo         The PAYPAL-AUTH-ALGO header value
     * @param payload          The raw webhook payload
     * @return true if the signature is valid, false otherwise
     */
    public boolean verifyWebhookSignature(
            String transmissionId,
            String transmissionTime,
            String transmissionSig,
            String certUrl,
            String authAlgo,
            String payload) {

        String webhookId = payPalConfig.getWebhookId();

        if (webhookId == null || webhookId.isBlank()) {
            log.error("PayPal webhook ID is not configured. Cannot verify webhook signature.");
            throw new IllegalStateException("PayPal webhook ID is not configured");
        }

        // Check if all required headers are present
        if (isAnyHeaderMissing(transmissionId, transmissionTime, transmissionSig, certUrl, authAlgo)) {
            log.warn("Missing required PayPal webhook headers for signature verification");
            return false;
        }

        try {
            // Parse the payload to get it as a JSON object
            JsonNode webhookEvent = objectMapper.readTree(payload);

            // Build the verification request body
            String verificationRequest = buildVerificationRequest(
                    transmissionId, transmissionTime, transmissionSig,
                    certUrl, authAlgo, webhookId, webhookEvent);

            log.debug("Verifying PayPal webhook signature for transmission ID: {}", transmissionId);

            // Call PayPal's verify-webhook-signature endpoint
            String response = paypalRestClient.post()
                    .uri("/v1/notifications/verify-webhook-signature")
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(verificationRequest)
                    .retrieve()
                    .body(String.class);

            // Parse the response
            JsonNode responseJson = objectMapper.readTree(response);
            String verificationStatus = responseJson.path("verification_status").asText();

            boolean isValid = VERIFICATION_STATUS_SUCCESS.equals(verificationStatus);

            if (isValid) {
                log.info("PayPal webhook signature verification successful for transmission ID: {}",
                        transmissionId);
            } else {
                log.warn("PayPal webhook signature verification failed. Status: {}, Transmission ID: {}",
                        verificationStatus, transmissionId);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying PayPal webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if any of the required headers are missing.
     */
    private boolean isAnyHeaderMissing(String... headers) {
        for (String header : headers) {
            if (header == null || header.isBlank()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build the JSON request body for webhook signature verification.
     */
    private String buildVerificationRequest(
            String transmissionId,
            String transmissionTime,
            String transmissionSig,
            String certUrl,
            String authAlgo,
            String webhookId,
            JsonNode webhookEvent) throws Exception {

        // Build the request object manually to ensure correct JSON structure
        var requestNode = objectMapper.createObjectNode();
        requestNode.put("auth_algo", authAlgo);
        requestNode.put("cert_url", certUrl);
        requestNode.put("transmission_id", transmissionId);
        requestNode.put("transmission_sig", transmissionSig);
        requestNode.put("transmission_time", transmissionTime);
        requestNode.put("webhook_id", webhookId);
        requestNode.set("webhook_event", webhookEvent);

        return objectMapper.writeValueAsString(requestNode);
    }
}
