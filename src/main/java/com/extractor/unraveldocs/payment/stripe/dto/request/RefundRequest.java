package com.extractor.unraveldocs.payment.stripe.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for processing refunds
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    
    @NotBlank(message = "Payment intent ID is required")
    private String paymentIntentId;
    
    @Min(value = 0, message = "Amount must be positive")
    private Long amount; // Optional - if null, full refund
    
    private String reason; // duplicate, fraudulent, or requested_by_customer
}
