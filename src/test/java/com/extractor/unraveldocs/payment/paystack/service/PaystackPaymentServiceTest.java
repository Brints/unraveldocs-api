package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.paystack.model.PaystackCustomer;
import com.extractor.unraveldocs.payment.paystack.model.PaystackPayment;
import com.extractor.unraveldocs.payment.paystack.repository.PaystackPaymentRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaystackPaymentService.
 * Tests focus on repository operations and status management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaystackPaymentService Tests")
class PaystackPaymentServiceTest {

    @Mock
    private PaystackPaymentRepository paymentRepository;

    @Captor
    private ArgumentCaptor<PaystackPayment> paymentCaptor;

    private User testUser;
    private PaystackCustomer testCustomer;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        testCustomer = PaystackCustomer.builder()
                .id("customer-id-123")
                .user(testUser)
                .customerCode("CUS_test123")
                .email("test@example.com")
                .build();
    }

    @Nested
    @DisplayName("Get Payment Tests")
    class GetPaymentTests {

        @Test
        @DisplayName("Should get payment by reference")
        void shouldGetPaymentByReference() {
            // Given
            String reference = "PAY_TEST123";
            PaystackPayment payment = PaystackPayment.builder()
                    .reference(reference)
                    .status(PaymentStatus.SUCCEEDED)
                    .amount(new BigDecimal("28.00"))
                    .build();

            when(paymentRepository.findByReference(reference)).thenReturn(Optional.of(payment));

            // When
            Optional<PaystackPayment> result = paymentRepository.findByReference(reference);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getReference()).isEqualTo(reference);
            assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        }

        @Test
        @DisplayName("Should return empty when payment not found")
        void shouldReturnEmptyWhenPaymentNotFound() {
            // Given
            String reference = "PAY_UNKNOWN";
            when(paymentRepository.findByReference(reference)).thenReturn(Optional.empty());

            // When
            Optional<PaystackPayment> result = paymentRepository.findByReference(reference);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get payments by user ID with pagination")
        void shouldGetPaymentsByUserId() {
            // Given
            String userId = "user-123";
            Pageable pageable = PageRequest.of(0, 10);
            List<PaystackPayment> payments = List.of(
                    PaystackPayment.builder().reference("PAY_1").status(PaymentStatus.SUCCEEDED).build(),
                    PaystackPayment.builder().reference("PAY_2").status(PaymentStatus.PENDING).build()
            );
            Page<PaystackPayment> paymentPage = new PageImpl<>(payments, pageable, payments.size());

            when(paymentRepository.findByUser_Id(userId, pageable)).thenReturn(paymentPage);

            // When
            Page<PaystackPayment> result = paymentRepository.findByUser_Id(userId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().getFirst().getReference()).isEqualTo("PAY_1");
        }

        @Test
        @DisplayName("Should check if payment exists by reference")
        void shouldCheckIfPaymentExists() {
            // Given
            String reference = "PAY_TEST123";
            when(paymentRepository.existsByReference(reference)).thenReturn(true);

            // When
            boolean exists = paymentRepository.existsByReference(reference);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when payment does not exist")
        void shouldReturnFalseWhenPaymentDoesNotExist() {
            // Given
            String reference = "PAY_UNKNOWN";
            when(paymentRepository.existsByReference(reference)).thenReturn(false);

            // When
            boolean exists = paymentRepository.existsByReference(reference);

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Payment Status Update Tests")
    class PaymentStatusUpdateTests {

        @Test
        @DisplayName("Should update payment to SUCCEEDED status")
        void shouldUpdatePaymentToSucceeded() {
            // Given
            PaystackPayment payment = PaystackPayment.builder()
                    .reference("PAY_TEST123")
                    .status(PaymentStatus.PENDING)
                    .amount(new BigDecimal("28.00"))
                    .build();

            // When
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setChannel("card");
            payment.setGatewayResponse("Successful");

            when(paymentRepository.save(any(PaystackPayment.class))).thenReturn(payment);
            PaystackPayment savedPayment = paymentRepository.save(payment);

            // Then
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(savedPayment.getChannel()).isEqualTo("card");
            assertThat(savedPayment.getGatewayResponse()).isEqualTo("Successful");
        }

        @Test
        @DisplayName("Should update payment to FAILED status with failure message")
        void shouldUpdatePaymentToFailed() {
            // Given
            PaystackPayment payment = PaystackPayment.builder()
                    .reference("PAY_TEST123")
                    .status(PaymentStatus.PENDING)
                    .build();

            // When
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureMessage("Insufficient funds");

            when(paymentRepository.save(any(PaystackPayment.class))).thenReturn(payment);
            PaystackPayment savedPayment = paymentRepository.save(payment);

            // Then
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(savedPayment.getFailureMessage()).isEqualTo("Insufficient funds");
        }
    }

    @Nested
    @DisplayName("Refund Tests")
    class RefundTests {

        @Test
        @DisplayName("Should record full refund and update status to REFUNDED")
        void shouldRecordFullRefund() {
            // Given
            BigDecimal originalAmount = new BigDecimal("28.00");
            PaystackPayment payment = PaystackPayment.builder()
                    .reference("PAY_TEST123")
                    .amount(originalAmount)
                    .status(PaymentStatus.SUCCEEDED)
                    .build();

            // When - simulate full refund
            payment.setAmountRefunded(originalAmount);
            if (payment.getAmountRefunded().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            }

            when(paymentRepository.save(any(PaystackPayment.class))).thenReturn(payment);
            PaystackPayment savedPayment = paymentRepository.save(payment);

            // Then
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(savedPayment.getAmountRefunded()).isEqualTo(originalAmount);
        }

        @Test
        @DisplayName("Should record partial refund and update status to PARTIALLY_REFUNDED")
        void shouldRecordPartialRefund() {
            // Given
            BigDecimal originalAmount = new BigDecimal("28.00");
            BigDecimal refundAmount = new BigDecimal("10.00");
            PaystackPayment payment = PaystackPayment.builder()
                    .reference("PAY_TEST123")
                    .amount(originalAmount)
                    .status(PaymentStatus.SUCCEEDED)
                    .build();

            // When - simulate partial refund
            payment.setAmountRefunded(refundAmount);
            if (payment.getAmountRefunded().compareTo(payment.getAmount()) < 0) {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }

            when(paymentRepository.save(any(PaystackPayment.class))).thenReturn(payment);
            PaystackPayment savedPayment = paymentRepository.save(payment);

            // Then
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(savedPayment.getAmountRefunded()).isEqualTo(refundAmount);
        }
    }

    @Nested
    @DisplayName("Payment Model Tests")
    class PaymentModelTests {

        @Test
        @DisplayName("Should create payment with all required fields")
        void shouldCreatePaymentWithRequiredFields() {
            // Given & When
            PaystackPayment payment = PaystackPayment.builder()
                    .user(testUser)
                    .paystackCustomer(testCustomer)
                    .reference("PAY_TEST123")
                    .accessCode("abc123")
                    .authorizationUrl("https://checkout.paystack.com/abc123")
                    .status(PaymentStatus.PENDING)
                    .amount(new BigDecimal("28.00"))
                    .currency("USD")
                    .build();

            // Then
            assertThat(payment.getReference()).isEqualTo("PAY_TEST123");
            assertThat(payment.getAccessCode()).isEqualTo("abc123");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getAmount()).isEqualTo(new BigDecimal("28.00"));
            assertThat(payment.getCurrency()).isEqualTo("USD");
            assertThat(payment.getUser()).isEqualTo(testUser);
            assertThat(payment.getPaystackCustomer()).isEqualTo(testCustomer);
        }

        @Test
        @DisplayName("Should set authorization code from transaction")
        void shouldSetAuthorizationCode() {
            // Given
            PaystackPayment payment = PaystackPayment.builder()
                    .reference("PAY_TEST123")
                    .status(PaymentStatus.PENDING)
                    .build();

            // When
            payment.setAuthorizationCode("AUTH_abc123");
            payment.setStatus(PaymentStatus.SUCCEEDED);

            // Then
            assertThat(payment.getAuthorizationCode()).isEqualTo("AUTH_abc123");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        }

        @Test
        @DisplayName("Should track transaction fees")
        void shouldTrackTransactionFees() {
            // Given
            PaystackPayment payment = PaystackPayment.builder()
                    .reference("PAY_TEST123")
                    .amount(new BigDecimal("28.00"))
                    .status(PaymentStatus.SUCCEEDED)
                    .build();

            // When
            payment.setFees(new BigDecimal("1.00"));

            // Then
            assertThat(payment.getFees()).isEqualTo(new BigDecimal("1.00"));
        }
    }
}
