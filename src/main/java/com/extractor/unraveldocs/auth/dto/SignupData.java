package com.extractor.unraveldocs.auth.dto;

import com.extractor.unraveldocs.auth.datamodel.Role;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Date;

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
        boolean termsAccepted,
        boolean marketingOptIn,
        String country,
        String profession,
        String organization,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
