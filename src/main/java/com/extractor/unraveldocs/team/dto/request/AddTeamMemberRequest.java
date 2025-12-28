package com.extractor.unraveldocs.team.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for adding a member to a team.
 */
public record AddTeamMemberRequest(
        @NotBlank(message = "User ID is required") String userId) {
}
