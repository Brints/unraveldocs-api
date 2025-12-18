package com.extractor.unraveldocs.payment.paystack.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for verifying a Paystack transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyTransactionRequest {

    @NotBlank(message = "Transaction reference is required")
    private String reference;
}

