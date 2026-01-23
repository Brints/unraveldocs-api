package com.extractor.unraveldocs.payment.paypal.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Configuration class for PayPal API settings.
 * Supports both sandbox and live environments.
 */
@Slf4j
@Getter
@Configuration
public class PayPalConfig {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Value("${paypal.currency:USD}")
    private String defaultCurrency;

    @Value("${paypal.webhook.id:}")
    private String webhookId;

    @Value("${paypal.base.url}/success")
    private String returnUrl;

    @Value("${paypal.base.url}/cancel")
    private String cancelUrl;

    /**
     * Get the PayPal API base URL based on the mode (sandbox or live).
     */
    public String getBaseUrl() {
        return "sandbox".equalsIgnoreCase(mode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    /**
     * Generate Basic Auth header for OAuth2 token requests.
     */
    public String getBasicAuthHeader() {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create a RestClient for PayPal API calls.
     * Note: This client does not include authentication by default.
     * Authentication should be handled per-request using OAuth2 tokens.
     */
    @Bean
    public RestClient paypalRestClient() {
        log.info("Initializing PayPal RestClient in {} mode", mode);
        return RestClient.builder()
                .baseUrl(getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
