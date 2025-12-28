package com.extractor.unraveldocs.team.repository;

import com.extractor.unraveldocs.team.datamodel.TeamMemberRole;
import com.extractor.unraveldocs.team.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {

    List<TeamMember> findByTeamId(String teamId);

    List<TeamMember> findByUserId(String userId);

    Optional<TeamMember> findByTeamIdAndUserId(String teamId, String userId);

    boolean existsByTeamIdAndUserId(String teamId, String userId);

    long countByTeamId(String teamId);

    @Modifying
    @Query("DELETE FROM TeamMember m WHERE m.team.id = :teamId AND m.user.id IN :userIds")
    void deleteByTeamIdAndUserIdIn(@Param("teamId") String teamId,
            @Param("userIds") List<String> userIds);

    @Modifying
    @Query("DELETE FROM TeamMember m WHERE m.team.id = :teamId AND m.user.id = :userId")
    void deleteByTeamIdAndUserId(@Param("teamId") String teamId, @Param("userId") String userId);

    @Query("SELECT m FROM TeamMember m WHERE m.team.id = :teamId AND m.role = :role")
    List<TeamMember> findMembersByTeamIdAndRole(@Param("teamId") String teamId,
            @Param("role") TeamMemberRole role);

    @Query("SELECT m FROM TeamMember m WHERE m.team.id = :teamId AND m.role IN :roles")
    List<TeamMember> findByTeamIdAndRoleIn(@Param("teamId") String teamId,
            @Param("roles") List<TeamMemberRole> roles);

    @Query("SELECT COUNT(m) FROM TeamMember m WHERE m.team.id = :teamId AND m.role = :role")
    long countByTeamIdAndRole(@Param("teamId") String teamId, @Param("role") TeamMemberRole role);

    Optional<TeamMember> findFirstByTeamIdAndRole(String teamId, TeamMemberRole role);
}
