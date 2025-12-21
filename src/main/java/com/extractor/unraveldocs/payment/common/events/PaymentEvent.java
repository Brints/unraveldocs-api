package com.extractor.unraveldocs.payment.common.events;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Base event for all payment-related events published to Kafka.
 * Designed for high-throughput processing (thousands of TPS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent implements Serializable {

    /**
     * Unique event ID for idempotency
     */
    private String eventId;

    /**
     * Type of payment event
     */
    private PaymentEventType eventType;

    /**
     * Payment provider (STRIPE, PAYSTACK, PAYPAL, FLUTTERWAVE, CHAPPA)
     */
    private PaymentGateway provider;

    /**
     * Provider's unique payment/transaction ID
     */
    private String providerPaymentId;

    /**
     * Internal payment reference
     */
    private String paymentReference;

    /**
     * User ID associated with this payment
     */
    private String userId;

    /**
     * Payment amount
     */
    private BigDecimal amount;

    /**
     * Currency code (USD, NGN, EUR, etc.)
     */
    private String currency;

    /**
     * Current payment status
     */
    private PaymentStatus status;

    /**
     * Previous status (for status change events)
     */
    private PaymentStatus previousStatus;

    /**
     * Payment method (card, bank_transfer, mobile_money, etc.)
     */
    private String paymentMethod;

    /**
     * Subscription ID if this is a subscription payment
     */
    private String subscriptionId;

    /**
     * Plan ID if this is a subscription payment
     */
    private String planId;

    /**
     * Timestamp when the event occurred
     */
    private OffsetDateTime occurredAt;

    /**
     * Additional metadata from the payment provider
     */
    private Map<String, Object> metadata;

    /**
     * Error message if payment failed
     */
    private String errorMessage;

    /**
     * Error code if payment failed
     */
    private String errorCode;

    /**
     * Webhook event ID from provider (for deduplication)
     */
    private String webhookEventId;
}

