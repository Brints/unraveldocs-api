package com.extractor.unraveldocs.payment.common.dto;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Unified response DTO for payment operations across all providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private PaymentGateway provider;

    /**
     * Internal payment ID
     */
    private String id;

    /**
     * Provider-specific payment ID (e.g., Stripe payment_intent ID)
     */
    private String providerPaymentId;

    private PaymentStatus status;

    private BigDecimal amount;

    private String currency;

    /**
     * Client secret for frontend payment confirmation (Stripe)
     */
    private String clientSecret;

    /**
     * Payment URL for redirect-based flows (Paystack)
     */
    private String paymentUrl;

    /**
     * Receipt URL after successful payment
     */
    private String receiptUrl;

    private String description;

    private OffsetDateTime createdAt;

    private boolean success;

    private String errorMessage;
}
