package com.extractor.unraveldocs.team.jobs;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.team.service.TeamBillingService;
import com.extractor.unraveldocs.team.service.TeamMemberSubscriptionSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamSubscriptionChargeJobTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamBillingService teamBillingService;

    @Mock
    private TeamMemberSubscriptionSyncService memberSubscriptionSyncService;

    @Mock
    private SanitizeLogging sanitizer;

    @InjectMocks
    private TeamSubscriptionChargeJob teamSubscriptionChargeJob;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(teamSubscriptionChargeJob, "pastDueGraceDays", 7);
        when(sanitizer.sanitizeLogging(any())).thenReturn("value");
        when(sanitizer.sanitizeLoggingInteger(any())).thenReturn("0");

        when(teamRepository.findTeamsWithExpiredTrialForAutoCharge(any())).thenReturn(Collections.emptyList());
        when(teamRepository.findTeamsDueForBilling(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("processSubscriptionCharges downgrades members when teams expire")
    void processSubscriptionChargesDowngradesOnExpiry() {
        Team expiredTrialTeam = team("team-a", TeamSubscriptionStatus.TRIAL);
        Team cancelledTeam = team("team-b", TeamSubscriptionStatus.CANCELLED);

        when(teamRepository.findTeamsWithExpiredTrialNoAutoRenew(any())).thenReturn(List.of(expiredTrialTeam));
        when(teamRepository.findCancelledTeamsReadyToExpire(any())).thenReturn(List.of(cancelledTeam));
        when(teamRepository.findPastDueTeamsBeyondGrace(any())).thenReturn(Collections.emptyList());

        teamSubscriptionChargeJob.processSubscriptionCharges();

        verify(memberSubscriptionSyncService).downgradeTeamMembers(expiredTrialTeam);
        verify(memberSubscriptionSyncService).downgradeTeamMembers(cancelledTeam);
    }

    @Test
    @DisplayName("processSubscriptionCharges expires past-due teams after grace period")
    void processSubscriptionChargesExpiresPastDueTeamsAfterGrace() {
        Team pastDueTeam = team("team-c", TeamSubscriptionStatus.PAST_DUE);
        pastDueTeam.setPastDueSince(OffsetDateTime.now().minusDays(8));

        when(teamRepository.findTeamsWithExpiredTrialNoAutoRenew(any())).thenReturn(Collections.emptyList());
        when(teamRepository.findCancelledTeamsReadyToExpire(any())).thenReturn(Collections.emptyList());
        when(teamRepository.findPastDueTeamsBeyondGrace(any())).thenReturn(List.of(pastDueTeam));

        teamSubscriptionChargeJob.processSubscriptionCharges();

        verify(teamRepository).save(pastDueTeam);
        verify(memberSubscriptionSyncService).downgradeTeamMembers(pastDueTeam);
    }

    private Team team(String code, TeamSubscriptionStatus status) {
        Team team = new Team();
        team.setId(code + "-id");
        team.setTeamCode(code);
        team.setSubscriptionStatus(status);
        team.setBillingCycle(TeamBillingCycle.MONTHLY);
        team.setActive(true);
        return team;
    }
}

