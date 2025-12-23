package com.extractor.unraveldocs.payment.receipt.events;

import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.core.MessageResult;
import com.extractor.unraveldocs.brokers.kafka.producer.KafkaMessageProducer;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReceiptEventPublisher
 */
class ReceiptEventPublisherTest {

    @Mock
    private KafkaMessageProducer<BaseEvent<ReceiptRequestedEvent>> kafkaMessageProducer;

    @Mock
    private ReceiptEventMapper receiptEventMapper;

    private ReceiptEventPublisher receiptEventPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        receiptEventPublisher = new ReceiptEventPublisher(kafkaMessageProducer, receiptEventMapper);
    }

    @Test
    @DisplayName("Should publish receipt request to Kafka")
    void shouldPublishReceiptRequestToKafka() {
        // Given
        ReceiptData receiptData = createTestReceiptData();
        ReceiptRequestedEvent event = createTestReceiptRequestedEvent();

        when(receiptEventMapper.toReceiptRequestedEvent(any(ReceiptData.class))).thenReturn(event);
        when(kafkaMessageProducer.send(any())).thenReturn(
                CompletableFuture.completedFuture(MessageResult.success("msg-id", "unraveldocs-receipts", 0, 0L))
        );

        // When
        receiptEventPublisher.publishReceiptRequest(receiptData);

        // Then
        verify(receiptEventMapper).toReceiptRequestedEvent(receiptData);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<BaseEvent<ReceiptRequestedEvent>>> messageCaptor =
                ArgumentCaptor.forClass(Message.class);
        verify(kafkaMessageProducer).send(messageCaptor.capture());

        Message<BaseEvent<ReceiptRequestedEvent>> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.topic()).isEqualTo("unraveldocs-receipts");
        assertThat(capturedMessage.key()).isEqualTo("pi_test123");
        assertThat(capturedMessage.payload().getPayload()).isEqualTo(event);
        assertThat(capturedMessage.payload().getMetadata().getEventType()).isEqualTo("ReceiptRequested");
    }

    @Test
    @DisplayName("Should use external payment ID as partition key")
    void shouldUseExternalPaymentIdAsPartitionKey() {
        // Given
        ReceiptData receiptData = createTestReceiptData();
        ReceiptRequestedEvent event = createTestReceiptRequestedEvent();

        when(receiptEventMapper.toReceiptRequestedEvent(any(ReceiptData.class))).thenReturn(event);
        when(kafkaMessageProducer.send(any())).thenReturn(
                CompletableFuture.completedFuture(MessageResult.success("msg-id", "unraveldocs-receipts", 0, 0L))
        );

        // When
        receiptEventPublisher.publishReceiptRequest(receiptData);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<BaseEvent<ReceiptRequestedEvent>>> messageCaptor =
                ArgumentCaptor.forClass(Message.class);
        verify(kafkaMessageProducer).send(messageCaptor.capture());

        assertThat(messageCaptor.getValue().key()).isEqualTo(receiptData.getExternalPaymentId());
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
}

