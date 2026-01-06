package com.extractor.unraveldocs.payment.paypal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for PayPal capture operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCaptureResponse {

    private String id;
    private String orderId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;

    /**
     * PayPal transaction ID for the capture.
     */
    private String transactionId;

    /**
     * Final capture indicator.
     */
    private boolean finalCapture;

    /**
     * Seller protection status.
     */
    private String sellerProtection;

    /**
     * Invoice ID if provided.
     */
    private String invoiceId;

    /**
     * Check if the capture was successful.
     */
    public boolean isSuccessful() {
        return "COMPLETED".equals(status);
    }

    /**
     * Check if the capture is pending.
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }
}
