package com.extractor.unraveldocs.shared.request;

import com.extractor.unraveldocs.auth.datamodel.Role;

import java.time.OffsetDateTime;

public interface UserDataProjection {
    void setId(String id);
    void setProfilePicture(String profilePicture);
    void setFirstName(String firstName);
    void setLastName(String lastName);
    void setEmail(String email);
    void setLastLogin(OffsetDateTime lastLogin);
    void setRole(Role role);
    void setVerified(boolean verified);
    void setCountry(String country);
    void setProfession(String profession);
    void setOrganization(String organization);
    void setCreatedAt(OffsetDateTime createdAt);
    void setUpdatedAt(OffsetDateTime updatedAt);
}
