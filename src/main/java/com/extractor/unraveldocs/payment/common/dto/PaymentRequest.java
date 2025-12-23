package com.extractor.unraveldocs.payment.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Unified request DTO for creating payments across all payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    private String currency;

    private String description;

    private String receiptEmail;

    /**
     * For providers that require a payment method ID
     */
    private String paymentMethodId;

    /**
     * Custom metadata to attach to the payment
     */
    private Map<String, String> metadata;

    /**
     * Idempotency key to prevent duplicate payments
     */
    private String idempotencyKey;
}
