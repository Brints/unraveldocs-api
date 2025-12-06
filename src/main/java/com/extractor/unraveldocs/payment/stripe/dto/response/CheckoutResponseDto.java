package com.extractor.unraveldocs.payment.stripe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for checkout session creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDto {
    private String sessionId;
    private String sessionUrl;
    private String customerId;
    private Long expiresAt;
}
