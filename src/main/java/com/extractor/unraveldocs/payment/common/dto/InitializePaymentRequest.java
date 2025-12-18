package com.extractor.unraveldocs.payment.common.dto;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Common request DTO for initializing payments across different payment gateways.
 * This provides a unified interface regardless of whether Stripe or Paystack is used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializePaymentRequest {

    @NotNull(message = "Payment gateway is required")
    private PaymentGateway gateway;

    @NotBlank(message = "Plan name is required")
    private String planName;

    @Positive(message = "Amount must be positive")
    private Long amountInCents;

    private String currency;

    private String callbackUrl;

    private String cancelUrl;

    private Map<String, Object> metadata;
}
