package com.extractor.unraveldocs.team.datamodel;

import lombok.Getter;

@Getter
public enum OrganizationMemberRole {
    OWNER("Owner"),
    ADMIN("Admin"),
    MEMBER("Member");

    private final String displayName;

    OrganizationMemberRole(String displayName) {
        this.displayName = displayName;
    }

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canPromoteToAdmin() {
        return this == OWNER;
    }

    public boolean canCloseOrganization() {
        return this == OWNER;
    }
}
