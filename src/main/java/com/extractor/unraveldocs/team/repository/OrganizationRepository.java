package com.extractor.unraveldocs.team.repository;

import com.extractor.unraveldocs.team.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findByOrgCode(String orgCode);

    Optional<Organization> findByIdAndIsActiveTrue(String id);

    Optional<Organization> findByIdAndIsClosedFalse(String id);

    List<Organization> findByCreatedById(String userId);

    @Query("SELECT o FROM Organization o WHERE o.createdBy.id = :userId AND o.isClosed = false")
    List<Organization> findActiveOrganizationsByCreator(@Param("userId") String userId);

    boolean existsByOrgCode(String orgCode);

    boolean existsByName(String name);

    @Query("SELECT o FROM Organization o JOIN o.members m WHERE m.user.id = :userId AND o.isClosed = false")
    List<Organization> findActiveOrganizationsForUser(@Param("userId") String userId);

    @Query("SELECT o FROM Organization o JOIN o.members m WHERE m.user.id = :userId")
    List<Organization> findAllOrganizationsForUser(@Param("userId") String userId);

    @Query("SELECT COUNT(o) FROM Organization o WHERE o.createdBy.id = :userId AND o.isClosed = false")
    long countActiveOrganizationsByCreator(@Param("userId") String userId);
}
