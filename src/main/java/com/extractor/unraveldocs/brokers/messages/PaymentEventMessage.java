package com.extractor.unraveldocs.brokers.messages;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Message for payment-related events.
 * Used to propagate payment status changes across the system.
 *
 * @param eventType Type of payment event
 * @param paymentId Internal payment ID
 * @param providerPaymentId Payment provider's payment ID
 * @param provider Payment provider (STRIPE, PAYSTACK, etc.)
 * @param userId User who made the payment
 * @param amount Payment amount
 * @param currency Currency code (USD, NGN, etc.)
 * @param status Current payment status
 * @param subscriptionId Associated subscription ID (if applicable)
 * @param metadata Additional event metadata
 * @param eventTimestamp When the event occurred
 */
public record PaymentEventMessage(
        PaymentEventType eventType,
        String paymentId,
        String providerPaymentId,
        String provider,
        String userId,
        BigDecimal amount,
        String currency,
        String status,
        String subscriptionId,
        Map<String, String> metadata,
        Instant eventTimestamp
) {
    
    public enum PaymentEventType {
        PAYMENT_INITIATED,
        PAYMENT_SUCCEEDED,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED,
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_RENEWED,
        SUBSCRIPTION_CANCELLED,
        SUBSCRIPTION_EXPIRED,
        INVOICE_PAID,
        INVOICE_FAILED
    }
    
    /**
     * Create a payment success event.
     */
    public static PaymentEventMessage paymentSucceeded(
            String paymentId,
            String providerPaymentId,
            String provider,
            String userId,
            BigDecimal amount,
            String currency
    ) {
        return new PaymentEventMessage(
                PaymentEventType.PAYMENT_SUCCEEDED,
                paymentId,
                providerPaymentId,
                provider,
                userId,
                amount,
                currency,
                "succeeded",
                null,
                Map.of(),
                Instant.now()
        );
    }
    
    /**
     * Create a payment failure event.
     */
    public static PaymentEventMessage paymentFailed(
            String paymentId,
            String providerPaymentId,
            String provider,
            String userId,
            String failureReason
    ) {
        return new PaymentEventMessage(
                PaymentEventType.PAYMENT_FAILED,
                paymentId,
                providerPaymentId,
                provider,
                userId,
                null,
                null,
                "failed",
                null,
                Map.of("failure_reason", failureReason),
                Instant.now()
        );
    }
    
    /**
     * Create a subscription created event.
     */
    public static PaymentEventMessage subscriptionCreated(
            String paymentId,
            String providerPaymentId,
            String provider,
            String userId,
            String subscriptionId,
            BigDecimal amount,
            String currency
    ) {
        return new PaymentEventMessage(
                PaymentEventType.SUBSCRIPTION_CREATED,
                paymentId,
                providerPaymentId,
                provider,
                userId,
                amount,
                currency,
                "active",
                subscriptionId,
                Map.of(),
                Instant.now()
        );
    }
    
    /**
     * Create a refund event.
     */
    public static PaymentEventMessage refunded(
            String paymentId,
            String providerPaymentId,
            String provider,
            String userId,
            BigDecimal refundAmount,
            String currency
    ) {
        return new PaymentEventMessage(
                PaymentEventType.PAYMENT_REFUNDED,
                paymentId,
                providerPaymentId,
                provider,
                userId,
                refundAmount,
                currency,
                "refunded",
                null,
                Map.of(),
                Instant.now()
        );
    }
    
    /**
     * Add metadata to the event.
     */
    public PaymentEventMessage withMetadata(Map<String, String> metadata) {
        return new PaymentEventMessage(
                eventType,
                paymentId,
                providerPaymentId,
                provider,
                userId,
                amount,
                currency,
                status,
                subscriptionId,
                metadata,
                eventTimestamp
        );
    }
}
