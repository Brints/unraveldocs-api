package com.extractor.unraveldocs.payment.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Unified request DTO for creating subscriptions across all payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    @NotBlank(message = "Price ID is required")
    private String priceId;

    /**
     * Plan code for providers that use plan codes (Paystack)
     */
    private String planCode;

    @Positive(message = "Quantity must be positive")
    private Long quantity;

    /**
     * Trial period in days
     */
    private Integer trialPeriodDays;

    /**
     * Payment method ID for immediate charge
     */
    private String paymentMethodId;

    /**
     * Success redirect URL
     */
    private String successUrl;

    /**
     * Cancel redirect URL
     */
    private String cancelUrl;

    /**
     * Custom metadata
     */
    private Map<String, String> metadata;
}
