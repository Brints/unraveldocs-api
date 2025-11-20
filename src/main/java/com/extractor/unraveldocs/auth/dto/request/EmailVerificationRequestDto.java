package com.extractor.unraveldocs.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record EmailVerificationRequestDto(
        @NotNull(message = "Email cannot be null")
        @Schema(description = "Email verification token", example = "test@example.com")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Token cannot be null")
        @Schema(
                description = "Verification token",
                example = "fc2dec961bfbcfb224e3d56b6516a2986904323c6520db012c2516e1e8170a3b4559772399831afc"
        )
        String token
) {
}
