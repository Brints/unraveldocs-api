package com.extractor.unraveldocs.credit.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new credit pack (Admin).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCreditPackRequest {

    @NotBlank(message = "Pack name is required")
    private String name;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotNull(message = "Price in cents is required")
    @Min(value = 1, message = "Price must be at least 1 cent")
    private Long priceInCents;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Credits included is required")
    @Min(value = 1, message = "Credits must be at least 1")
    private Integer creditsIncluded;
}
