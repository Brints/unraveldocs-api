package com.extractor.unraveldocs.team.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Request DTO for adding a member to a team.
 */
@Builder
public record AddTeamMemberRequest(
                @NotBlank(message = "Email is required")
                @Email(message = "Invalid email format")
                String email
) {
}
