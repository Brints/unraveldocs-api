package com.extractor.unraveldocs.team.model;

import com.extractor.unraveldocs.team.datamodel.OrganizationMemberRole;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "organization_members", indexes = {
        @Index(columnList = "organization_id"),
        @Index(columnList = "user_id"),
        @Index(columnList = "role")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "organization_id", "user_id" })
})
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationMemberRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id")
    private User invitedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "joined_at")
    private OffsetDateTime joinedAt;

    // Helper methods
    public boolean isOwner() {
        return role == OrganizationMemberRole.OWNER;
    }

    public boolean isAdmin() {
        return role == OrganizationMemberRole.ADMIN;
    }

    public boolean isMember() {
        return role == OrganizationMemberRole.MEMBER;
    }

    public boolean canManageMembers() {
        return role.canManageMembers();
    }

    public boolean canPromoteToAdmin() {
        return role.canPromoteToAdmin();
    }

    public boolean canCloseOrganization() {
        return role.canCloseOrganization();
    }
}
