package com.extractor.unraveldocs.credit.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for transferring credits to another user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferCreditsRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Transfer amount must be at least 1")
    private Integer amount;
}
