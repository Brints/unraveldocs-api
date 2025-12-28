package com.extractor.unraveldocs.organization.repository;

import com.extractor.unraveldocs.organization.datamodel.InvitationStatus;
import com.extractor.unraveldocs.organization.model.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, String> {

    Optional<OrganizationInvitation> findByInvitationToken(String invitationToken);

    List<OrganizationInvitation> findByOrganizationId(String organizationId);

    List<OrganizationInvitation> findByOrganizationIdAndStatus(String organizationId, InvitationStatus status);

    List<OrganizationInvitation> findByEmail(String email);

    Optional<OrganizationInvitation> findByOrganizationIdAndEmailAndStatus(String organizationId, String email,
            InvitationStatus status);

    boolean existsByOrganizationIdAndEmailAndStatus(String organizationId, String email, InvitationStatus status);

    @Query("SELECT i FROM OrganizationInvitation i WHERE i.status = :status AND i.expiresAt < :now")
    List<OrganizationInvitation> findExpiredInvitations(@Param("status") InvitationStatus status,
            @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE OrganizationInvitation i SET i.status = 'EXPIRED' WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    int expirePendingInvitations(@Param("now") OffsetDateTime now);

    long countByOrganizationIdAndStatus(String organizationId, InvitationStatus status);
}
