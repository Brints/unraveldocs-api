package com.extractor.unraveldocs.team.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for adding a member to a team.
 */
public record AddTeamMemberRequest(
                @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {
}
