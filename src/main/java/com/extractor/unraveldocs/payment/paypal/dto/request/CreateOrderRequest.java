package com.extractor.unraveldocs.payment.paypal.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for creating a PayPal order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String description;

    private String returnUrl;

    private String cancelUrl;

    /**
     * Additional metadata to store with the order.
     */
    private Map<String, String> metadata;

    /**
     * Subscription plan ID for subscription payments.
     */
    private String planId;

    /**
     * Intent: CAPTURE for immediate payment, AUTHORIZE for auth-only.
     */
    @Builder.Default
    private String intent = "CAPTURE";
}
