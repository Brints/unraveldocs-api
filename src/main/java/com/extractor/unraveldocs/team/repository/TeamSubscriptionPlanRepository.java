package com.extractor.unraveldocs.team.repository;

import com.extractor.unraveldocs.team.model.TeamSubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamSubscriptionPlanRepository extends JpaRepository<TeamSubscriptionPlan, String> {

    Optional<TeamSubscriptionPlan> findByName(String name);

    @Query("SELECT p FROM TeamSubscriptionPlan p WHERE p.name = :name AND p.isActive = true")
    Optional<TeamSubscriptionPlan> findActiveByName(String name);

    @Query("SELECT p FROM TeamSubscriptionPlan p WHERE p.isActive = true ORDER BY p.monthlyPrice ASC")
    List<TeamSubscriptionPlan> findAllActive();

    boolean existsByName(String name);
}
