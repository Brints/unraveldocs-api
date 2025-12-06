package com.extractor.unraveldocs.payment.stripe.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a payment intent (one-time payment)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentRequest {
    
    @NotNull(message = "Amount is required")
    @Min(value = 50, message = "Amount must be at least 50 cents")
    private Long amount; // Amount in cents
    
    @NotBlank(message = "Currency is required")
    private String currency = "usd";
    
    private List<String> paymentMethodTypes = List.of("card");
    
    private String description;
    
    private String receiptEmail;
    
    private Map<String, String> metadata;
    
    private Boolean captureMethod; // Auto or manual
}
