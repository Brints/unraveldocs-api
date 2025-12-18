package com.extractor.unraveldocs.payment.common.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified request DTO for refund operations across all payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    /**
     * Provider-specific payment ID to refund
     */
    private String paymentId;

    /**
     * Amount to refund in smallest currency unit (cents).
     * If null, full refund is performed.
     */
    @Positive(message = "Amount must be positive")
    private Long amount;

    /**
     * Reason for the refund
     */
    private String reason;
}
