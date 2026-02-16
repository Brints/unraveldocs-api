package com.extractor.unraveldocs.credit.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a credit pack (Admin).
 * All fields are optional — only provided fields are updated.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCreditPackRequest {

    private String displayName;

    @Min(value = 1, message = "Price must be at least 1 cent")
    private Long priceInCents;

    @Min(value = 1, message = "Credits must be at least 1")
    private Integer creditsIncluded;

    private Boolean isActive;
}
