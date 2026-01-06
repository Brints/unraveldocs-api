package com.extractor.unraveldocs.payment.paypal.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a PayPal subscription.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotBlank(message = "Plan ID is required")
    private String planId;

    private String returnUrl;

    private String cancelUrl;

    /**
     * Custom ID for tracking the subscription.
     */
    private String customId;

    /**
     * Start time for the subscription (ISO 8601 format).
     */
    private String startTime;

    /**
     * Quantity of the plan to subscribe to.
     */
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Whether to auto-renew the subscription.
     */
    @Builder.Default
    private Boolean autoRenewal = true;
}
