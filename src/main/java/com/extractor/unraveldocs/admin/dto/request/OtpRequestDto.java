package com.extractor.unraveldocs.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OtpRequestDto {
    @Schema(description = "Length of the OTP to be generated", example = "6")
    @Min(value = 6, message = "Length must be at least 6")
    @Max(value = 10, message = "Length must be at most 10")
    private int length;

    @Schema(description = "Number of OTPs to generate", example = "1")
    @Min(value = 1, message = "Count must be at least 1")
    private int count;
}
