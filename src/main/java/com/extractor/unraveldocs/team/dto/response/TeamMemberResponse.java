package com.extractor.unraveldocs.team.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Response DTO for team member details.
 * Email is masked for non-owners viewing other members.
 */
@Data
@Builder
public class TeamMemberResponse {
    private String id;
    private String userId;
    private String firstName;
    private String lastName;
    private String email; // Masked for non-owners
    private String role;
    private OffsetDateTime joinedAt;
}
