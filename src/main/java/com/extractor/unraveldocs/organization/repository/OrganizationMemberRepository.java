package com.extractor.unraveldocs.organization.repository;

import com.extractor.unraveldocs.organization.datamodel.OrganizationMemberRole;
import com.extractor.unraveldocs.organization.model.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, String> {

        List<OrganizationMember> findByOrganizationId(String organizationId);

        List<OrganizationMember> findByUserId(String userId);

        Optional<OrganizationMember> findByOrganizationIdAndUserId(String organizationId, String userId);

        boolean existsByOrganizationIdAndUserId(String organizationId, String userId);

        long countByOrganizationId(String organizationId);

        @Modifying
        @Query("DELETE FROM OrganizationMember m WHERE m.organization.id = :orgId AND m.user.id IN :userIds")
        void deleteByOrganizationIdAndUserIdIn(@Param("orgId") String organizationId,
                        @Param("userIds") List<String> userIds);

        @Modifying
        @Query("DELETE FROM OrganizationMember m WHERE m.organization.id = :orgId AND m.user.id = :userId")
        void deleteByOrganizationIdAndUserId(@Param("orgId") String organizationId, @Param("userId") String userId);

        @Query("SELECT m FROM OrganizationMember m WHERE m.organization.id = :orgId AND m.role = :role")
        List<OrganizationMember> findMembersByOrganizationIdAndRole(@Param("orgId") String organizationId,
                        @Param("role") OrganizationMemberRole role);

        @Query("SELECT m FROM OrganizationMember m WHERE m.organization.id = :orgId AND m.role IN :roles")
        List<OrganizationMember> findByOrganizationIdAndRoleIn(@Param("orgId") String organizationId,
                        @Param("roles") List<OrganizationMemberRole> roles);

        @Query("SELECT COUNT(m) FROM OrganizationMember m WHERE m.organization.id = :orgId AND m.role = :role")
        long countByOrganizationIdAndRole(@Param("orgId") String organizationId,
                        @Param("role") OrganizationMemberRole role);

        Optional<OrganizationMember> findFirstByOrganizationIdAndRole(String organizationId,
                        OrganizationMemberRole role);
}
