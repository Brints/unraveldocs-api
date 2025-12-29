package com.extractor.unraveldocs.team.repository;

import com.extractor.unraveldocs.shared.datamodel.InvitationStatus;
import com.extractor.unraveldocs.team.model.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, String> {

    Optional<TeamInvitation> findByInvitationToken(String token);

    List<TeamInvitation> findByTeamId(String teamId);

    List<TeamInvitation> findByEmail(String email);

    @Query("SELECT i FROM TeamInvitation i WHERE i.team.id = :teamId AND i.email = :email AND i.status = 'PENDING'")
    Optional<TeamInvitation> findPendingInvitation(@Param("teamId") String teamId, @Param("email") String email);

    @Query("SELECT i FROM TeamInvitation i WHERE i.team.id = :teamId AND i.email = :email AND i.status = 'PENDING' AND i.expiresAt > :now")
    Optional<TeamInvitation> findPendingByTeamIdAndEmail(@Param("teamId") String teamId, @Param("email") String email,
            @Param("now") OffsetDateTime now);

    @Query("SELECT i FROM TeamInvitation i WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    List<TeamInvitation> findExpiredPendingInvitations(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE TeamInvitation i SET i.status = 'EXPIRED' WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    int markExpiredInvitations(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM TeamInvitation i WHERE i.status IN ('EXPIRED', 'CANCELLED', 'ACCEPTED') AND i.createdAt < :before")
    int cleanupOldInvitations(@Param("before") OffsetDateTime before);

    long countByTeamIdAndStatus(String teamId, InvitationStatus status);
}
