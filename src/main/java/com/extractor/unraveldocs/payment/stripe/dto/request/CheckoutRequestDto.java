package com.extractor.unraveldocs.payment.stripe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a Stripe checkout session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDto {
    
    @NotBlank(message = "Price ID is required")
    private String priceId;
    
    private String successUrl;
    
    private String cancelUrl;
    
    private String mode = "subscription"; // "subscription" or "payment"
    
    private Long quantity = 1L;
    
    private Map<String, String> metadata;
    
    private Integer trialPeriodDays;
    
    private String promoCode;
}
