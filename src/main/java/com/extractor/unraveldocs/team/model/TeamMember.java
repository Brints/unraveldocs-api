package com.extractor.unraveldocs.team.model;

import com.extractor.unraveldocs.team.datamodel.TeamMemberRole;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "team_members", indexes = {
        @Index(columnList = "team_id"),
        @Index(columnList = "user_id"),
        @Index(columnList = "role")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "team_id", "user_id" })
})
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamMemberRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id")
    private User invitedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "joined_at")
    private OffsetDateTime joinedAt;

    // Helper methods
    public boolean isOwner() {
        return role == TeamMemberRole.OWNER;
    }

    public boolean isAdmin() {
        return role == TeamMemberRole.ADMIN;
    }

    public boolean isMember() {
        return role == TeamMemberRole.MEMBER;
    }

    public boolean canManageMembers() {
        return role.isCanManageMembers();
    }

    public boolean canPromoteToAdmin() {
        return role.isCanPromoteToAdmin();
    }

    public boolean canCloseTeam() {
        return role.isCanCloseTeam();
    }
}
