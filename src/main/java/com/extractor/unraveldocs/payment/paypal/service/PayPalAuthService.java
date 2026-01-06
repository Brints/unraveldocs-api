package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.payment.paypal.config.PayPalConfig;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalAccessTokenResponse;
import com.extractor.unraveldocs.payment.paypal.exception.PayPalPaymentException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for managing PayPal OAuth2 access tokens.
 * Handles token caching and refresh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalAuthService {

    private final PayPalConfig payPalConfig;
    private final RestClient paypalRestClient;
    private final ObjectMapper objectMapper;

    private String cachedAccessToken;
    private Instant tokenExpirationTime;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 60;

    /**
     * Get a valid access token, refreshing if necessary.
     */
    public String getAccessToken() {
        tokenLock.lock();
        try {
            if (isTokenValid()) {
                return cachedAccessToken;
            }
            return refreshAccessToken();
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Get the authorization header value for API calls.
     */
    public String getAuthorizationHeader() {
        return "Bearer " + getAccessToken();
    }

    /**
     * Check if the cached token is still valid.
     */
    private boolean isTokenValid() {
        return cachedAccessToken != null
                && tokenExpirationTime != null
                && Instant.now().isBefore(tokenExpirationTime);
    }

    /**
     * Refresh the access token from PayPal.
     */
    private String refreshAccessToken() {
        log.debug("Refreshing PayPal access token");

        try {
            String response = paypalRestClient.post()
                    .uri("/v1/oauth2/token")
                    .header(HttpHeaders.AUTHORIZATION, payPalConfig.getBasicAuthHeader())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(String.class);

            JsonNode jsonNode = objectMapper.readTree(response);

            PayPalAccessTokenResponse tokenResponse = PayPalAccessTokenResponse.builder()
                    .accessToken(jsonNode.get("access_token").asText())
                    .tokenType(jsonNode.has("token_type") ? jsonNode.get("token_type").asText() : "Bearer")
                    .expiresIn(jsonNode.has("expires_in") ? jsonNode.get("expires_in").asInt() : 3600)
                    .scope(jsonNode.has("scope") ? jsonNode.get("scope").asText() : null)
                    .appId(jsonNode.has("app_id") ? jsonNode.get("app_id").asText() : null)
                    .nonce(jsonNode.has("nonce") ? jsonNode.get("nonce").asText() : null)
                    .build();

            if (!tokenResponse.isValid()) {
                throw new PayPalPaymentException("Invalid token response from PayPal");
            }

            cachedAccessToken = tokenResponse.getAccessToken();
            tokenExpirationTime = Instant.now()
                    .plusSeconds(tokenResponse.getExpiresIn() - TOKEN_EXPIRY_BUFFER_SECONDS);

            log.info("Successfully refreshed PayPal access token, expires in {} seconds",
                    tokenResponse.getExpiresIn());

            return cachedAccessToken;

        } catch (PayPalPaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to refresh PayPal access token: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to obtain PayPal access token", e);
        }
    }

    /**
     * Invalidate the cached token (for testing or manual refresh).
     */
    public void invalidateToken() {
        tokenLock.lock();
        try {
            cachedAccessToken = null;
            tokenExpirationTime = null;
            log.debug("PayPal access token invalidated");
        } finally {
            tokenLock.unlock();
        }
    }
}
