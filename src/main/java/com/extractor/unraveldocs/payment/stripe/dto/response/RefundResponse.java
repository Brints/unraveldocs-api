package com.extractor.unraveldocs.payment.stripe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for refund operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    
    private String id;
    
    private String status;
    
    private Long amount;
    
    private String currency;
    
    private String reason;
    
    private String paymentIntentId;
    
    private Long createdAt;
}
