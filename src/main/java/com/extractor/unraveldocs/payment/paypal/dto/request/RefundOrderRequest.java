package com.extractor.unraveldocs.payment.paypal.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for processing a PayPal refund.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrderRequest {

    @NotBlank(message = "Capture ID is required")
    private String captureId;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String currency;

    private String reason;

    /**
     * Invoice ID for reference.
     */
    private String invoiceId;

    /**
     * Note to include with the refund.
     */
    private String noteToPayer;
}
