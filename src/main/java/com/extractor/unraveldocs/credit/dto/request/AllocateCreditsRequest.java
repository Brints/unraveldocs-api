package com.extractor.unraveldocs.credit.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin credit allocation to a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocateCreditsRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Allocation amount must be at least 1")
    private Integer amount;

    private String reason;
}
