package com.extractor.unraveldocs.admin.dto;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.shared.request.UserDataProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminData implements UserDataProjection {
    private String id;
    private String profilePicture;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private OffsetDateTime lastLogin;
    private boolean isVerified;
    private String country;
    private String profession;
    private String organization;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
