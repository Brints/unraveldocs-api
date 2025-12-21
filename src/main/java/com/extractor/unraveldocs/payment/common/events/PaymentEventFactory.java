package com.extractor.unraveldocs.payment.common.events;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Factory for creating PaymentEvent instances.
 * Provides convenience methods for common event types.
 */
@Slf4j
@Component
public class PaymentEventFactory {

    /**
     * Create a payment succeeded event.
     */
    public PaymentEvent paymentSucceeded(
            PaymentGateway provider,
            String providerPaymentId,
            String paymentReference,
            String userId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String webhookEventId,
            Map<String, Object> metadata
    ) {
        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(PaymentEventType.PAYMENT_SUCCEEDED)
                .provider(provider)
                .providerPaymentId(providerPaymentId)
                .paymentReference(paymentReference)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.SUCCEEDED)
                .paymentMethod(paymentMethod)
                .webhookEventId(webhookEventId)
                .metadata(metadata)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Create a payment failed event.
     */
    public PaymentEvent paymentFailed(
            PaymentGateway provider,
            String providerPaymentId,
            String paymentReference,
            String userId,
            BigDecimal amount,
            String currency,
            String errorCode,
            String errorMessage,
            String webhookEventId
    ) {
        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(PaymentEventType.PAYMENT_FAILED)
                .provider(provider)
                .providerPaymentId(providerPaymentId)
                .paymentReference(paymentReference)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.FAILED)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .webhookEventId(webhookEventId)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Create a payment initiated event.
     */
    public PaymentEvent paymentInitiated(
            PaymentGateway provider,
            String providerPaymentId,
            String paymentReference,
            String userId,
            BigDecimal amount,
            String currency,
            String paymentMethod
    ) {
        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(PaymentEventType.PAYMENT_INITIATED)
                .provider(provider)
                .providerPaymentId(providerPaymentId)
                .paymentReference(paymentReference)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Create a refund succeeded event.
     */
    public PaymentEvent refundSucceeded(
            PaymentGateway provider,
            String providerPaymentId,
            String paymentReference,
            String userId,
            BigDecimal refundAmount,
            String currency,
            boolean isPartial,
            String webhookEventId
    ) {
        PaymentEventType eventType = isPartial
                ? PaymentEventType.PARTIAL_REFUND_SUCCEEDED
                : PaymentEventType.REFUND_SUCCEEDED;

        PaymentStatus status = isPartial
                ? PaymentStatus.PARTIALLY_REFUNDED
                : PaymentStatus.REFUNDED;

        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(eventType)
                .provider(provider)
                .providerPaymentId(providerPaymentId)
                .paymentReference(paymentReference)
                .userId(userId)
                .amount(refundAmount)
                .currency(currency)
                .status(status)
                .webhookEventId(webhookEventId)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Create a subscription created event.
     */
    public PaymentEvent subscriptionCreated(
            PaymentGateway provider,
            String subscriptionId,
            String userId,
            String planId,
            BigDecimal amount,
            String currency,
            String webhookEventId
    ) {
        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(PaymentEventType.SUBSCRIPTION_CREATED)
                .provider(provider)
                .subscriptionId(subscriptionId)
                .userId(userId)
                .planId(planId)
                .amount(amount)
                .currency(currency)
                .webhookEventId(webhookEventId)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Create a subscription renewed event.
     */
    public PaymentEvent subscriptionRenewed(
            PaymentGateway provider,
            String subscriptionId,
            String providerPaymentId,
            String userId,
            String planId,
            BigDecimal amount,
            String currency,
            String webhookEventId
    ) {
        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(PaymentEventType.SUBSCRIPTION_RENEWED)
                .provider(provider)
                .subscriptionId(subscriptionId)
                .providerPaymentId(providerPaymentId)
                .userId(userId)
                .planId(planId)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.SUCCEEDED)
                .webhookEventId(webhookEventId)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Create a subscription canceled event.
     */
    public PaymentEvent subscriptionCanceled(
            PaymentGateway provider,
            String subscriptionId,
            String userId,
            String planId,
            String webhookEventId
    ) {
        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(PaymentEventType.SUBSCRIPTION_CANCELED)
                .provider(provider)
                .subscriptionId(subscriptionId)
                .userId(userId)
                .planId(planId)
                .webhookEventId(webhookEventId)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Create a dispute/chargeback event.
     */
    public PaymentEvent disputeCreated(
            PaymentGateway provider,
            String providerPaymentId,
            String paymentReference,
            String userId,
            BigDecimal disputeAmount,
            String currency,
            String reason,
            String webhookEventId
    ) {
        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(PaymentEventType.DISPUTE_CREATED)
                .provider(provider)
                .providerPaymentId(providerPaymentId)
                .paymentReference(paymentReference)
                .userId(userId)
                .amount(disputeAmount)
                .currency(currency)
                .errorMessage(reason)
                .webhookEventId(webhookEventId)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Create a raw webhook received event (for audit/replay).
     */
    public PaymentEvent webhookReceived(
            PaymentGateway provider,
            String webhookEventId,
            String eventType,
            Map<String, Object> rawPayload
    ) {
        return PaymentEvent.builder()
                .eventId(generateEventId())
                .eventType(PaymentEventType.WEBHOOK_RECEIVED)
                .provider(provider)
                .webhookEventId(webhookEventId)
                .metadata(Map.of(
                        "webhookType", eventType,
                        "rawPayload", rawPayload
                ))
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    private String generateEventId() {
        return UUID.randomUUID().toString();
    }
}

