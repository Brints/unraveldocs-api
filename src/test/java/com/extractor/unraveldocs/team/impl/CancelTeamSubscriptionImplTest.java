package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionType;
import com.extractor.unraveldocs.team.dto.response.TeamResponse;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.team.service.TeamBillingService;
import com.extractor.unraveldocs.team.service.TeamMemberSubscriptionSyncService;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelTeamSubscriptionImplTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamBillingService teamBillingService;

    @Mock
    private TeamMemberSubscriptionSyncService memberSubscriptionSyncService;

    @Mock
    private ResponseBuilderService responseBuilder;

    @Mock
    private SanitizeLogging sanitizer;

    @InjectMocks
    private CancelTeamSubscriptionImpl cancelTeamSubscription;

    private User owner;
    private Team team;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId("owner-1");
        owner.setEmail("owner@example.com");

        team = new Team();
        team.setId("team-1");
        team.setName("Acme");
        team.setTeamCode("T1234567");
        team.setBillingCycle(TeamBillingCycle.MONTHLY);
        team.setSubscriptionType(TeamSubscriptionType.TEAM_PREMIUM);
        team.setSubscriptionStatus(TeamSubscriptionStatus.TRIAL);
        team.setTrialEndsAt(OffsetDateTime.now().plusDays(5));
        team.setCreatedBy(owner);
        team.setActive(true);

        TeamMember ownerMember = new TeamMember();
        ownerMember.setRole(MemberRole.OWNER);
        ownerMember.setTeam(team);
        ownerMember.setUser(owner);

        when(sanitizer.sanitizeLogging(any())).thenReturn("value");
        when(sanitizer.sanitizeLoggingObject(any())).thenReturn("value");
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findFirstByTeamIdAndRole(team.getId(), MemberRole.OWNER)).thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.countByTeamId(team.getId())).thenReturn(1L);
        when(teamBillingService.cancelSubscription(team)).thenReturn(true);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UnravelDocsResponse<TeamResponse> response = new UnravelDocsResponse<>();
        when(responseBuilder.buildUserResponse(any(TeamResponse.class), any(), anyString())).thenReturn(response);
    }

    @Test
    @DisplayName("cancelSubscription preserves trial end as subscription end for trial cancellations")
    void cancelSubscriptionPreservesTrialEndForTrialStatus() {
        OffsetDateTime expectedEnd = team.getTrialEndsAt();

        cancelTeamSubscription.cancelSubscription(team.getId(), owner);

        assertThat(team.getSubscriptionStatus()).isEqualTo(TeamSubscriptionStatus.CANCELLED);
        assertThat(team.getSubscriptionEndsAt()).isEqualTo(expectedEnd);
        verify(memberSubscriptionSyncService, never()).downgradeTeamMembers(any(Team.class));
    }
}


