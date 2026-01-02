package com.extractor.unraveldocs.payment.receipt.events;

import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.service.ReceiptGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReceiptMessageListener
 */
class ReceiptMessageListenerTest {

        @Mock
        private ReceiptGenerationService receiptGenerationService;

        @Mock
        private ReceiptEventMapper receiptEventMapper;

        @Mock
        private Acknowledgment acknowledgment;

        private ReceiptMessageListener receiptMessageListener;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                objectMapper = new ObjectMapper();
                objectMapper.findAndRegisterModules();
                receiptMessageListener = new ReceiptMessageListener(
                                receiptGenerationService, receiptEventMapper, objectMapper);
        }

        @Test
        @DisplayName("Should process receipt request and acknowledge message")
        void shouldProcessReceiptRequestAndAcknowledge() {
                // Given
                ReceiptRequestedEvent payload = createTestReceiptRequestedEvent();
                BaseEvent<Object> event = createBaseEvent(payload);
                ReceiptData receiptData = createTestReceiptData();
                Receipt receipt = createTestReceipt();

                ConsumerRecord<String, BaseEvent<Object>> record = new ConsumerRecord<>(
                                "unraveldocs-receipts", 0, 0L, "pi_test123", event);

                when(receiptEventMapper.toReceiptData(any(ReceiptRequestedEvent.class))).thenReturn(receiptData);
                when(receiptGenerationService.processReceiptGeneration(receiptData)).thenReturn(receipt);

                // When
                receiptMessageListener.receiveReceiptRequestedEvent(record, acknowledgment);

                // Then
                verify(receiptEventMapper).toReceiptData(any(ReceiptRequestedEvent.class));
                verify(receiptGenerationService).processReceiptGeneration(receiptData);
                verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should not acknowledge message on processing failure")
        void shouldNotAcknowledgeOnFailure() {
                // Given
                ReceiptRequestedEvent payload = createTestReceiptRequestedEvent();
                BaseEvent<Object> event = createBaseEvent(payload);
                ReceiptData receiptData = createTestReceiptData();

                ConsumerRecord<String, BaseEvent<Object>> record = new ConsumerRecord<>(
                                "unraveldocs-receipts", 0, 0L, "pi_test123", event);

                when(receiptEventMapper.toReceiptData(any(ReceiptRequestedEvent.class))).thenReturn(receiptData);
                when(receiptGenerationService.processReceiptGeneration(any()))
                                .thenThrow(new RuntimeException("Generation failed"));

                // When/Then
                assertThatThrownBy(() -> receiptMessageListener.receiveReceiptRequestedEvent(record, acknowledgment))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessage("Generation failed");

                verify(acknowledgment, never()).acknowledge();
        }

        @Test
        @DisplayName("Should handle existing receipt gracefully")
        void shouldHandleExistingReceiptGracefully() {
                // Given
                ReceiptRequestedEvent payload = createTestReceiptRequestedEvent();
                BaseEvent<Object> event = createBaseEvent(payload);
                ReceiptData receiptData = createTestReceiptData();

                ConsumerRecord<String, BaseEvent<Object>> record = new ConsumerRecord<>(
                                "unraveldocs-receipts", 0, 0L, "pi_test123", event);

                when(receiptEventMapper.toReceiptData(any(ReceiptRequestedEvent.class))).thenReturn(receiptData);
                when(receiptGenerationService.processReceiptGeneration(receiptData)).thenReturn(null);

                // When
                receiptMessageListener.receiveReceiptRequestedEvent(record, acknowledgment);

                // Then
                verify(acknowledgment).acknowledge();
        }

        private BaseEvent<Object> createBaseEvent(ReceiptRequestedEvent payload) {
                BaseEvent<Object> event = new BaseEvent<>();
                event.setPayload(payload);
                event.setMetadata(null);
                return event;
        }

        private ReceiptRequestedEvent createTestReceiptRequestedEvent() {
                return ReceiptRequestedEvent.builder()
                                .userId("user-123")
                                .customerName("John Doe")
                                .customerEmail("john@example.com")
                                .paymentProvider(PaymentProvider.STRIPE)
                                .externalPaymentId("pi_test123")
                                .amount(BigDecimal.valueOf(99.99))
                                .currency("USD")
                                .paymentMethod("card")
                                .paymentMethodDetails("**** 4242")
                                .description("Premium Subscription")
                                .paidAt(OffsetDateTime.now())
                                .build();
        }

        private ReceiptData createTestReceiptData() {
                return ReceiptData.builder()
                                .userId("user-123")
                                .customerName("John Doe")
                                .customerEmail("john@example.com")
                                .paymentProvider(PaymentProvider.STRIPE)
                                .externalPaymentId("pi_test123")
                                .amount(BigDecimal.valueOf(99.99))
                                .currency("USD")
                                .paymentMethod("card")
                                .paymentMethodDetails("**** 4242")
                                .description("Premium Subscription")
                                .paidAt(OffsetDateTime.now())
                                .build();
        }

        private Receipt createTestReceipt() {
                return Receipt.builder()
                                .receiptNumber("RCP-20251221-000001")
                                .paymentProvider(PaymentProvider.STRIPE)
                                .externalPaymentId("pi_test123")
                                .amount(BigDecimal.valueOf(99.99))
                                .currency("USD")
                                .paymentMethod("card")
                                .build();
        }
}
