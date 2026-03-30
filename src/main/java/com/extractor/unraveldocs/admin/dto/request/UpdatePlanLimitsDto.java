package com.extractor.unraveldocs.admin.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlanLimitsDto {

    @NotNull(message = "Document upload limit is required")
    @Min(value = 0, message = "Limit cannot be negative")
    private Integer documentUploadLimit;

    @NotNull(message = "OCR page limit is required")
    @Min(value = 0, message = "Limit cannot be negative")
    private Integer ocrPageLimit;

    @NotNull(message = "Storage limit is required")
    @Min(value = 0, message = "Limit cannot be negative")
    private Long storageLimit;

    @NotNull(message = "AI operations limit is required")
    @Min(value = 0, message = "Limit cannot be negative")
    private Integer aiOperationsLimit;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Trial days is required")
    @Min(value = 0, message = "Trial days cannot be negative")
    private Integer trialDays;
}
