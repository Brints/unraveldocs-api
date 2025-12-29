package com.extractor.unraveldocs.organization.model;

import com.extractor.unraveldocs.shared.datamodel.InvitationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "organization_invitations", indexes = {
        @Index(columnList = "organization_id"),
        @Index(columnList = "invitation_token", unique = true),
        @Index(columnList = "email"),
        @Index(columnList = "status"),
        @Index(columnList = "expires_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String email;

    @Column(name = "invitation_token", nullable = false, unique = true)
    private String invitationToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "invited_by_id")
    private String invitedById;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    // Helper methods
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING && !isExpired();
    }

    public boolean canBeAccepted() {
        return status == InvitationStatus.PENDING && !isExpired();
    }

    public void markAsAccepted() {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = OffsetDateTime.now();
    }

    public void markAsExpired() {
        this.status = InvitationStatus.EXPIRED;
    }

    public void markAsCancelled() {
        this.status = InvitationStatus.CANCELLED;
    }
}
