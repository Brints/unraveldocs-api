package com.extractor.unraveldocs.admin.dto.response;

import com.extractor.unraveldocs.auth.datamodel.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Represents a summary of user information for administrative purposes.
 * This class is used to encapsulate user details in a concise format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
    private String id;
    private String profilePicture;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private boolean isActive;
    private boolean isVerified;
    private OffsetDateTime lastLogin;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
