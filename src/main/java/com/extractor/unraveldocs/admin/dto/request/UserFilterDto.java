package com.extractor.unraveldocs.admin.dto.request;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UserFilterDto {
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private Boolean isActive;
    private Boolean isVerified;
    private Boolean isPlatformAdmin;
    private Boolean isOrganizationAdmin;
    private String country;
    private String profession;
    private String organization;
    private SubscriptionPlans subscriptionPlan;
    private OffsetDateTime createdAfter;
    private OffsetDateTime createdBefore;
    private OffsetDateTime lastLoginAfter;
    private OffsetDateTime lastLoginBefore;
    private Boolean hasTeam;
    private Boolean isBlocked;

    @Min(value = 0, message = "Page must not be negative")
    private int page = 0;

    @Min(value = 1, message = "Size must be greater than 0")
    private int size = 10;

    @Pattern(
            regexp = "^(createdAt|lastLogin|email|firstName|lastName)$", message = "sortBy must be one of: " +
            "createdAt, lastLogin, email, firstName, lastName")
    private String sortBy = "createdAt";

    @Pattern(regexp = "^(asc|desc)$", message = "sortOrder must be either 'asc' or 'desc'")
    private String sortOrder = "asc";

    @Pattern(regexp = "^[a-zA-Z0-9_ ]*$", message = "searchTerm can only contain alphanumeric characters, underscores, and spaces")
    @Size(min = 3, message = "Search input must be at least 3 characters long")
    private String search;
}
