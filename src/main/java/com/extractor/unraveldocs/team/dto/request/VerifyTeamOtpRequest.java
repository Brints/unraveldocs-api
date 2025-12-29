package com.extractor.unraveldocs.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Request DTO for verifying OTP and completing team creation.
 */
@Builder
public record VerifyTeamOtpRequest(
        @NotBlank(message = "OTP is required") @Size(min = 6, max = 6, message = "OTP must be 6 digits") String otp) {
}
