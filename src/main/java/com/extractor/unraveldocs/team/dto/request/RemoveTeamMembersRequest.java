package com.extractor.unraveldocs.team.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for batch removing members from a team.
 */
public record RemoveTeamMembersRequest(
                @NotEmpty(message = "At least one member ID is required") List<String> memberIds) {
}
