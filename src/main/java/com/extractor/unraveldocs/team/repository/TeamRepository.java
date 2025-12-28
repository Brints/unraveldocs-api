package com.extractor.unraveldocs.team.repository;

import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, String> {

    Optional<Team> findByTeamCode(String teamCode);

    boolean existsByTeamCode(String teamCode);

    @Query("SELECT t FROM Team t WHERE t.createdBy.id = :userId")
    List<Team> findAllByCreatedById(@Param("userId") String userId);

    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.user.id = :userId")
    List<Team> findAllTeamsForUser(@Param("userId") String userId);

    @Query("SELECT t FROM Team t WHERE t.isActive = true AND t.isClosed = false")
    List<Team> findAllActiveTeams();

    // Trial management queries
    @Query("SELECT t FROM Team t WHERE t.subscriptionStatus = 'TRIAL' " +
            "AND t.trialReminderSent = false " +
            "AND t.trialEndsAt BETWEEN :now AND :reminderDate")
    List<Team> findTeamsNeedingTrialReminder(@Param("now") OffsetDateTime now,
            @Param("reminderDate") OffsetDateTime reminderDate);

    @Query("SELECT t FROM Team t WHERE t.subscriptionStatus = 'TRIAL' " +
            "AND t.trialEndsAt <= :now " +
            "AND t.autoRenew = true")
    List<Team> findTeamsWithExpiredTrialForAutoCharge(@Param("now") OffsetDateTime now);

    @Query("SELECT t FROM Team t WHERE t.subscriptionStatus = 'TRIAL' " +
            "AND t.trialEndsAt <= :now " +
            "AND t.autoRenew = false")
    List<Team> findTeamsWithExpiredTrialNoAutoRenew(@Param("now") OffsetDateTime now);

    // Billing queries
    @Query("SELECT t FROM Team t WHERE t.subscriptionStatus = 'ACTIVE' " +
            "AND t.autoRenew = true " +
            "AND t.nextBillingDate <= :now")
    List<Team> findTeamsDueForBilling(@Param("now") OffsetDateTime now);

    // Cancellation queries
    @Query("SELECT t FROM Team t WHERE t.subscriptionStatus = 'CANCELLED' " +
            "AND t.subscriptionEndsAt <= :now")
    List<Team> findCancelledTeamsReadyToExpire(@Param("now") OffsetDateTime now);

    // Admin queries
    @Query("SELECT t FROM Team t WHERE t.subscriptionStatus = :status")
    List<Team> findBySubscriptionStatus(@Param("status") TeamSubscriptionStatus status);

    @Modifying
    @Query("UPDATE Team t SET t.trialReminderSent = true WHERE t.id = :teamId")
    void markTrialReminderSent(@Param("teamId") String teamId);

    @Modifying
    @Query("UPDATE Team t SET t.subscriptionStatus = :status, t.updatedAt = :now WHERE t.id = :teamId")
    void updateSubscriptionStatus(@Param("teamId") String teamId,
            @Param("status") TeamSubscriptionStatus status,
            @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE Team t SET t.monthlyDocumentUploadCount = 0, " +
            "t.documentCountResetAt = :now, t.updatedAt = :now " +
            "WHERE t.isActive = true")
    void resetAllDocumentCounts(@Param("now") OffsetDateTime now);
}
