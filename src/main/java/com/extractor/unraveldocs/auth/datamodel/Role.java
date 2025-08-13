package com.extractor.unraveldocs.auth.datamodel;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import lombok.Getter;

@Getter
public enum Role {
    USER("user"),
    MODERATOR("moderator"),
    ADMIN("admin"),
    SUPER_ADMIN("super_admin");

    private final String role;

    Role(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return role;
    }

    public static Role fromString(String role) {
        for (Role r : Role.values()) {
            if (r.role.equalsIgnoreCase(role)) {
                return r;
            }
        }
        throw new BadRequestException("[" + role + "] is not a valid enum.");
    }

    public static String[] getValidRoles() {
        return new String[]{USER.role, MODERATOR.role, ADMIN.role, SUPER_ADMIN.role};
    }
}
