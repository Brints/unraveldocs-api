package com.extractor.unraveldocs.payment.stripe.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a subscription
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {
    
    @NotBlank(message = "Price ID is required")
    private String priceId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Long quantity = 1L;
    
    private Integer trialPeriodDays;
    
    private String promoCode;
    
    private String paymentBehavior = "default_incomplete"; // default_incomplete, error_if_incomplete, etc.
    
    private Map<String, String> metadata;
    
    private String defaultPaymentMethodId;
}
