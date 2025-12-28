package com.extractor.unraveldocs.team.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for inviting a member via email (Enterprise only).
 */
public record InviteTeamMemberRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {
}
