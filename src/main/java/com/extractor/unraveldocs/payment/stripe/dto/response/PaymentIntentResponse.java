package com.extractor.unraveldocs.payment.stripe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for payment intent operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    
    private String id;
    
    private String status;
    
    private Long amount;
    
    private String currency;
    
    private String clientSecret;
    
    private String paymentMethodId;
    
    private String receiptUrl;
    
    private String description;
    
    private Long createdAt;
}
