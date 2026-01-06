package com.extractor.unraveldocs.payment.paypal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for PayPal refund operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalRefundResponse {

    private String id;
    private String captureId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;

    /**
     * Reason for the refund.
     */
    private String reason;

    /**
     * Note to payer.
     */
    private String noteToPayer;

    /**
     * Invoice ID if provided.
     */
    private String invoiceId;

    /**
     * Check if the refund was successful.
     */
    public boolean isSuccessful() {
        return "COMPLETED".equals(status);
    }

    /**
     * Check if the refund is pending.
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }
}
