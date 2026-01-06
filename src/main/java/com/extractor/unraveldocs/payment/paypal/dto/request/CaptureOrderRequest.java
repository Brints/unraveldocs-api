package com.extractor.unraveldocs.payment.paypal.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for capturing a PayPal order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptureOrderRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    /**
     * Optional note to include with the capture.
     */
    private String note;

    /**
     * Final amount to capture (if different from authorized amount).
     */
    private String finalCaptureAmount;
}
