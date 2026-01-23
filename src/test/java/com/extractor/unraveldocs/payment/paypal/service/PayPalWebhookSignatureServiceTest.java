package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.payment.paypal.config.PayPalConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PayPalWebhookSignatureService.
 * Tests validation logic and configuration handling.
 * 
 * Note: Full integration tests with actual PayPal API verification
 * should be done in an integration test environment.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayPalWebhookSignatureService Tests")
class PayPalWebhookSignatureServiceTest {

    @Mock
    private PayPalConfig payPalConfig;

    @Mock
    private PayPalAuthService authService;

    @Mock
    private RestClient paypalRestClient;

    private ObjectMapper objectMapper;
    private PayPalWebhookSignatureService signatureService;

    // Test constants
    private static final String TEST_WEBHOOK_ID = "2G52649339764952T";
    private static final String TEST_TRANSMISSION_ID = "test-transmission-id";
    private static final String TEST_TRANSMISSION_TIME = "2026-01-17T17:00:00Z";
    private static final String TEST_TRANSMISSION_SIG = "test-signature";
    private static final String TEST_CERT_URL = "https://api.sandbox.paypal.com/v1/notifications/certs/CERT-123";
    private static final String TEST_AUTH_ALGO = "SHA256withRSA";
    private static final String TEST_PAYLOAD = "{\"id\":\"WH-123\",\"event_type\":\"PAYMENT.CAPTURE.COMPLETED\",\"resource\":{}}";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        signatureService = new PayPalWebhookSignatureService(
                payPalConfig, authService, paypalRestClient, objectMapper);
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should throw IllegalStateException when webhook ID is not configured")
        void shouldThrowWhenWebhookIdNotConfigured() {
            // Given
            when(payPalConfig.getWebhookId()).thenReturn("");

            // When/Then
            assertThatThrownBy(() -> signatureService.verifyWebhookSignature(
                    TEST_TRANSMISSION_ID, TEST_TRANSMISSION_TIME, TEST_TRANSMISSION_SIG,
                    TEST_CERT_URL, TEST_AUTH_ALGO, TEST_PAYLOAD))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("webhook ID is not configured");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when webhook ID is null")
        void shouldThrowWhenWebhookIdIsNull() {
            // Given
            when(payPalConfig.getWebhookId()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> signatureService.verifyWebhookSignature(
                    TEST_TRANSMISSION_ID, TEST_TRANSMISSION_TIME, TEST_TRANSMISSION_SIG,
                    TEST_CERT_URL, TEST_AUTH_ALGO, TEST_PAYLOAD))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("webhook ID is not configured");
        }
    }

    @Nested
    @DisplayName("Missing Header Validation Tests")
    class MissingHeaderValidationTests {

        @Test
        @DisplayName("Should return false when transmission ID is null")
        void shouldReturnFalseWhenTransmissionIdIsNull() {
            // Given
            when(payPalConfig.getWebhookId()).thenReturn(TEST_WEBHOOK_ID);

            // When
            boolean result = signatureService.verifyWebhookSignature(
                    null, TEST_TRANSMISSION_TIME, TEST_TRANSMISSION_SIG,
                    TEST_CERT_URL, TEST_AUTH_ALGO, TEST_PAYLOAD);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when transmission time is blank")
        void shouldReturnFalseWhenTransmissionTimeIsBlank() {
            // Given
            when(payPalConfig.getWebhookId()).thenReturn(TEST_WEBHOOK_ID);

            // When
            boolean result = signatureService.verifyWebhookSignature(
                    TEST_TRANSMISSION_ID, "   ", TEST_TRANSMISSION_SIG,
                    TEST_CERT_URL, TEST_AUTH_ALGO, TEST_PAYLOAD);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when transmission sig is null")
        void shouldReturnFalseWhenTransmissionSigIsNull() {
            // Given
            when(payPalConfig.getWebhookId()).thenReturn(TEST_WEBHOOK_ID);

            // When
            boolean result = signatureService.verifyWebhookSignature(
                    TEST_TRANSMISSION_ID, TEST_TRANSMISSION_TIME, null,
                    TEST_CERT_URL, TEST_AUTH_ALGO, TEST_PAYLOAD);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when cert URL is null")
        void shouldReturnFalseWhenCertUrlIsNull() {
            // Given
            when(payPalConfig.getWebhookId()).thenReturn(TEST_WEBHOOK_ID);

            // When
            boolean result = signatureService.verifyWebhookSignature(
                    TEST_TRANSMISSION_ID, TEST_TRANSMISSION_TIME, TEST_TRANSMISSION_SIG,
                    null, TEST_AUTH_ALGO, TEST_PAYLOAD);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when auth algo is empty")
        void shouldReturnFalseWhenAuthAlgoIsEmpty() {
            // Given
            when(payPalConfig.getWebhookId()).thenReturn(TEST_WEBHOOK_ID);

            // When
            boolean result = signatureService.verifyWebhookSignature(
                    TEST_TRANSMISSION_ID, TEST_TRANSMISSION_TIME, TEST_TRANSMISSION_SIG,
                    TEST_CERT_URL, "", TEST_PAYLOAD);

            // Then
            assertThat(result).isFalse();
        }
    }
}
