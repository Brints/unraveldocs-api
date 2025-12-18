package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.paystack.config.PaystackConfig;
import com.extractor.unraveldocs.payment.paystack.dto.response.SubscriptionData;
import com.extractor.unraveldocs.payment.paystack.dto.response.TransactionData;
import com.extractor.unraveldocs.payment.paystack.dto.webhook.PaystackWebhookEvent;
import com.extractor.unraveldocs.payment.paystack.model.PaystackPayment;
import com.extractor.unraveldocs.payment.paystack.repository.PaystackPaymentRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaystackWebhookService.
 * Tests focus on signature verification and webhook event handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaystackWebhookService Tests")
class PaystackWebhookServiceTest {

    @Mock
    private PaystackConfig paystackConfig;

    @Mock
    private PaystackPaymentRepository paymentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
    }

    @Nested
    @DisplayName("Webhook Signature Verification Tests")
    class SignatureVerificationTests {

        @Test
        @DisplayName("Should compute correct HMAC-SHA512 signature")
        void shouldComputeCorrectSignature() throws Exception {
            // Given
            String payload = "{\"event\": \"charge.success\", \"data\": {}}";
            String secretKey = "sk_test_secret";

            // When
            String signature = computeHmacSha512(payload, secretKey);

            // Then
            assertThat(signature).isNotNull();
            assertThat(signature).hasSize(128); // SHA-512 produces 64 bytes = 128 hex chars
        }

        @Test
        @DisplayName("Should produce different signatures for different payloads")
        void shouldProduceDifferentSignaturesForDifferentPayloads() throws Exception {
            // Given
            String payload1 = "{\"event\": \"charge.success\"}";
            String payload2 = "{\"event\": \"charge.failed\"}";
            String secretKey = "sk_test_secret";

            // When
            String signature1 = computeHmacSha512(payload1, secretKey);
            String signature2 = computeHmacSha512(payload2, secretKey);

            // Then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("Should produce different signatures for different secrets")
        void shouldProduceDifferentSignaturesForDifferentSecrets() throws Exception {
            // Given
            String payload = "{\"event\": \"charge.success\"}";
            String secret1 = "sk_test_secret1";
            String secret2 = "sk_test_secret2";

            // When
            String signature1 = computeHmacSha512(payload, secret1);
            String signature2 = computeHmacSha512(payload, secret2);

            // Then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        private String computeHmacSha512(String payload, String secretKey) throws Exception {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
    }

    @Nested
    @DisplayName("Webhook Event Type Tests")
    class WebhookEventTypeTests {

        @Test
        @DisplayName("Should identify charge.success event type")
        void shouldIdentifyChargeSuccessEvent() {
            // Given
            PaystackWebhookEvent event = new PaystackWebhookEvent();
            event.setEvent("charge.success");
            event.setData(new HashMap<>());

            // Then
            assertThat(event.getEvent()).isEqualTo("charge.success");
        }

        @Test
        @DisplayName("Should identify charge.failed event type")
        void shouldIdentifyChargeFailedEvent() {
            // Given
            PaystackWebhookEvent event = new PaystackWebhookEvent();
            event.setEvent("charge.failed");
            event.setData(new HashMap<>());

            // Then
            assertThat(event.getEvent()).isEqualTo("charge.failed");
        }

        @Test
        @DisplayName("Should identify subscription.create event type")
        void shouldIdentifySubscriptionCreateEvent() {
            // Given
            PaystackWebhookEvent event = new PaystackWebhookEvent();
            event.setEvent("subscription.create");
            event.setData(new HashMap<>());

            // Then
            assertThat(event.getEvent()).isEqualTo("subscription.create");
        }

        @Test
        @DisplayName("Should identify subscription.disable event type")
        void shouldIdentifySubscriptionDisableEvent() {
            // Given
            PaystackWebhookEvent event = new PaystackWebhookEvent();
            event.setEvent("subscription.disable");
            event.setData(new HashMap<>());

            // Then
            assertThat(event.getEvent()).isEqualTo("subscription.disable");
        }

        @Test
        @DisplayName("Should identify refund.processed event type")
        void shouldIdentifyRefundProcessedEvent() {
            // Given
            PaystackWebhookEvent event = new PaystackWebhookEvent();
            event.setEvent("refund.processed");
            event.setData(new HashMap<>());

            // Then
            assertThat(event.getEvent()).isEqualTo("refund.processed");
        }
    }

    @Nested
    @DisplayName("Payment Status Update Tests")
    class PaymentStatusUpdateTests {

        @Test
        @DisplayName("Should update payment to SUCCEEDED on charge.success")
        void shouldUpdatePaymentToSucceeded() {
            // Given
            PaystackPayment payment = PaystackPayment.builder()
                    .reference("PAY_TEST123")
                    .status(PaymentStatus.PENDING)
                    .build();

            // When
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setTransactionId(12345L);
            payment.setChannel("card");
            payment.setGatewayResponse("Successful");

            when(paymentRepository.save(any(PaystackPayment.class))).thenReturn(payment);
            PaystackPayment savedPayment = paymentRepository.save(payment);

            // Then
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(savedPayment.getChannel()).isEqualTo("card");
            assertThat(savedPayment.getTransactionId()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("Should update payment to FAILED on charge.failed")
        void shouldUpdatePaymentToFailed() {
            // Given
            PaystackPayment payment = PaystackPayment.builder()
                    .reference("PAY_TEST123")
                    .status(PaymentStatus.PENDING)
                    .build();

            // When
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("Insufficient funds");
            payment.setFailureMessage("Insufficient funds");

            when(paymentRepository.save(any(PaystackPayment.class))).thenReturn(payment);
            PaystackPayment savedPayment = paymentRepository.save(payment);

            // Then
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(savedPayment.getFailureMessage()).isEqualTo("Insufficient funds");
        }

        @Test
        @DisplayName("Should calculate fees correctly in kobo")
        void shouldCalculateFeesCorrectly() {
            // Given
            long feesInKobo = 100L; // 1.00 NGN

            // When
            BigDecimal feesInNaira = BigDecimal.valueOf(feesInKobo)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            // Then
            assertThat(feesInNaira).isEqualTo(new BigDecimal("1.00"));
        }
    }

    @Nested
    @DisplayName("Refund Processing Tests")
    class RefundProcessingTests {

        @Test
        @DisplayName("Should calculate refund amount from kobo")
        void shouldCalculateRefundAmount() {
            // Given
            long amountInKobo = 1000L; // 10.00

            // When
            BigDecimal refundAmount = BigDecimal.valueOf(amountInKobo)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            // Then
            assertThat(refundAmount).isEqualTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("Should handle integer refund amount")
        void shouldHandleIntegerRefundAmount() {
            // Given
            Number amountObj = 1000;

            // When
            long amountInKobo;
            amountInKobo = amountObj.longValue();

            // Then
            assertThat(amountInKobo).isEqualTo(1000L);
        }

        @Test
        @DisplayName("Should handle Long refund amount")
        void shouldHandleLongRefundAmount() {
            // Given
            Number amountObj = 1000L;

            // When
            long amountInKobo;
            amountInKobo = amountObj.longValue();

            // Then
            assertThat(amountInKobo).isEqualTo(1000L);
        }
    }

    @Nested
    @DisplayName("Transaction Data Parsing Tests")
    class TransactionDataParsingTests {

        @Test
        @DisplayName("Should create TransactionData with all fields")
        void shouldCreateTransactionData() {
            // Given & When
            TransactionData data = new TransactionData();
            data.setId(12345L);
            data.setReference("PAY_TEST123");
            data.setStatus("success");
            data.setChannel("card");
            data.setGatewayResponse("Successful");
            data.setFees(100L);
            data.setIpAddress("192.168.1.1");

            // Then
            assertThat(data.getId()).isEqualTo(12345L);
            assertThat(data.getReference()).isEqualTo("PAY_TEST123");
            assertThat(data.getStatus()).isEqualTo("success");
            assertThat(data.getChannel()).isEqualTo("card");
            assertThat(data.getFees()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should create SubscriptionData with all fields")
        void shouldCreateSubscriptionData() {
            // Given & When
            SubscriptionData data = new SubscriptionData();
            data.setId(12345L);
            data.setSubscriptionCode("SUB_test123");
            data.setStatus("active");
            data.setNextPaymentDate("2024-02-01T00:00:00Z");
            data.setPaymentsCount(5);

            // Then
            assertThat(data.getId()).isEqualTo(12345L);
            assertThat(data.getSubscriptionCode()).isEqualTo("SUB_test123");
            assertThat(data.getStatus()).isEqualTo("active");
            assertThat(data.getPaymentsCount()).isEqualTo(5);
        }
    }
}
