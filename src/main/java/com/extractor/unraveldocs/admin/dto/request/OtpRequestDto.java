package com.extractor.unraveldocs.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OtpRequestDto {
    @Schema(description = "Length of the OTP to be generated", example = "6")
    @Size(min = 6, max = 10, message = "OTP length must be between 6 and 10")
    private int length;

    @Schema(description = "Number of OTPs to generate", example = "1")
    @Min(value = 1, message = "Count must be at least 1")
    private int count;
}
