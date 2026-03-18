package com.extractor.unraveldocs.storage.service;

import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.storage.dto.StorageInfo;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionSource;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.model.TeamSubscriptionPlan;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageAllocationServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;

    @Mock
    private SanitizeLogging sanitizer;

    @InjectMocks
    private StorageAllocationService storageAllocationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-1");
    }

    @Test
    @DisplayName("getStorageInfo uses individual plan when subscription source is INDIVIDUAL even if team membership exists")
    void getStorageInfoUsesIndividualPlanWhenSourceIsIndividual() {
        SubscriptionPlan freePlan = new SubscriptionPlan();
        freePlan.setName(SubscriptionPlans.FREE);
        freePlan.setStorageLimit(125829120L);
        freePlan.setDocumentUploadLimit(5);
        freePlan.setOcrPageLimit(25);

        UserSubscription userSubscription = new UserSubscription();
        userSubscription.setUser(user);
        userSubscription.setPlan(freePlan);
        userSubscription.setStorageUsed(2048L);
        userSubscription.setOcrPagesUsed(1);
        userSubscription.setMonthlyDocumentsUploaded(2);
        userSubscription.setSubscriptionSource(SubscriptionSource.INDIVIDUAL);

        TeamSubscriptionPlan teamPlan = new TeamSubscriptionPlan();
        teamPlan.setDisplayName("Team Premium");
        teamPlan.setStorageLimit(214748364800L);
        teamPlan.setMonthlyDocumentLimit(200);

        Team team = new Team();
        team.setId("team-1");
        team.setSubscriptionStatus(TeamSubscriptionStatus.CANCELLED);
        team.setClosed(false);
        team.setPlan(teamPlan);
        team.setStorageUsed(10L);

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);

        when(userSubscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(userSubscription));
        when(userSubscriptionRepository.findByUserIdWithPlan(user.getId())).thenReturn(Optional.of(userSubscription));

        StorageInfo result = storageAllocationService.getStorageInfo(user);

        assertThat(result.getSubscriptionPlan()).isEqualTo("Free");
        assertThat(result.getStorageLimit()).isEqualTo(125829120L);
        verify(documentCollectionRepository, never()).countByUserId(user.getId());
    }

    @Test
    @DisplayName("getStorageInfo uses team plan when subscription source is TEAM and team access is allowed")
    void getStorageInfoUsesTeamPlanWhenSourceIsTeam() {
        SubscriptionPlan freePlan = new SubscriptionPlan();
        freePlan.setName(SubscriptionPlans.FREE);
        freePlan.setStorageLimit(125829120L);

        UserSubscription userSubscription = new UserSubscription();
        userSubscription.setUser(user);
        userSubscription.setPlan(freePlan);
        userSubscription.setSubscriptionSource(SubscriptionSource.TEAM);

        TeamSubscriptionPlan teamPlan = new TeamSubscriptionPlan();
        teamPlan.setDisplayName("Team Premium");
        teamPlan.setStorageLimit(214748364800L);
        teamPlan.setMonthlyDocumentLimit(200);

        Team team = new Team();
        team.setId("team-1");
        team.setSubscriptionStatus(TeamSubscriptionStatus.ACTIVE);
        team.setClosed(false);
        team.setPlan(teamPlan);
        team.setStorageUsed(1024L);

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);

        when(userSubscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(userSubscription));
        when(teamMemberRepository.findByUserId(user.getId())).thenReturn(List.of(member));
        when(documentCollectionRepository.countByUserId(user.getId())).thenReturn(3L);

        StorageInfo result = storageAllocationService.getStorageInfo(user);

        assertThat(result.getSubscriptionPlan()).isEqualTo("Team Premium");
        assertThat(result.getStorageLimit()).isEqualTo(214748364800L);
        assertThat(result.getDocumentUploadLimit()).isEqualTo(200);
        assertThat(result.getDocumentsUploaded()).isEqualTo(3);
    }
}




