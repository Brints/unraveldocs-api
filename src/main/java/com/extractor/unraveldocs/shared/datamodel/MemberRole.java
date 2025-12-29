package com.extractor.unraveldocs.shared.datamodel;

import lombok.Getter;

/**
 * Roles for team members.
 */
@Getter
public enum MemberRole {
    OWNER("Owner", true, true, true),
    ADMIN("Admin", true, false, false),
    MEMBER("Member", false, false, false);

    private final String displayName;
    private final boolean canManageMembers;
    private final boolean canPromoteToAdmin;
    private final boolean canCloseTeam;

    MemberRole(String displayName, boolean canManageMembers, boolean canPromoteToAdmin, boolean canCloseTeam) {
        this.displayName = displayName;
        this.canManageMembers = canManageMembers;
        this.canPromoteToAdmin = canPromoteToAdmin;
        this.canCloseTeam = canCloseTeam;
    }

    public static MemberRole fromString(String name) {
        for (MemberRole role : MemberRole.values()) {
            if (role.name().equalsIgnoreCase(name) || role.displayName.equalsIgnoreCase(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown team member role: " + name);
    }
}
