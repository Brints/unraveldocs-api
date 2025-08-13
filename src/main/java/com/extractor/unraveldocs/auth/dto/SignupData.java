package com.extractor.unraveldocs.auth.dto;

import com.extractor.unraveldocs.auth.datamodel.Role;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record SignupData(
        String id,
        String profilePicture,
        String firstName,
        String lastName,
        String email,
        Role role,
        OffsetDateTime lastLogin,
        boolean isActive,
        boolean isVerified,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
