package com.extractor.unraveldocs.team.service;

import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionSource;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionType;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamMemberSubscriptionSyncServiceImplTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TeamMemberSubscriptionSyncServiceImpl syncService;

    private User user;
    private SubscriptionPlan freePlan;
    private SubscriptionPlan proMonthly;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-1");

        freePlan = new SubscriptionPlan();
        freePlan.setId("plan-free");
        freePlan.setName(SubscriptionPlans.FREE);

        proMonthly = new SubscriptionPlan();
        proMonthly.setId("plan-pro-monthly");
        proMonthly.setName(SubscriptionPlans.PRO_MONTHLY);
    }

    @Test
    @DisplayName("upgradeTeamMember stores previous plan and marks subscription as team-managed")
    void upgradeTeamMemberStoresPreviousPlanAndMarksTeamSource() {
        UserSubscription existing = new UserSubscription();
        existing.setUser(user);
        existing.setPlan(freePlan);
        existing.setSubscriptionSource(SubscriptionSource.INDIVIDUAL);
        existing.setStatus("ACTIVE");

        Team team = new Team();
        team.setSubscriptionType(TeamSubscriptionType.TEAM_PREMIUM);

        when(userSubscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));
        when(subscriptionPlanRepository.findByName(SubscriptionPlans.PRO_MONTHLY)).thenReturn(Optional.of(proMonthly));
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        syncService.upgradeTeamMember(user, team);

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(userSubscriptionRepository).save(captor.capture());

        UserSubscription saved = captor.getValue();
        assertThat(saved.getPreviousPlan()).isEqualTo(freePlan);
        assertThat(saved.getPlan()).isEqualTo(proMonthly);
        assertThat(saved.getSubscriptionSource()).isEqualTo(SubscriptionSource.TEAM);
        assertThat(saved.getStatus()).isEqualTo("Active");
    }

    @Test
    @DisplayName("revertMemberSubscription restores previous plan and clears team source")
    void revertMemberSubscriptionRestoresPreviousPlan() {
        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setPlan(proMonthly);
        subscription.setPreviousPlan(freePlan);
        subscription.setSubscriptionSource(SubscriptionSource.TEAM);
        subscription.setStatus("ACTIVE");

        when(userSubscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        syncService.revertMemberSubscription(user);

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(userSubscriptionRepository).save(captor.capture());

        UserSubscription saved = captor.getValue();
        assertThat(saved.getPlan()).isEqualTo(freePlan);
        assertThat(saved.getPreviousPlan()).isNull();
        assertThat(saved.getSubscriptionSource()).isEqualTo(SubscriptionSource.INDIVIDUAL);
        assertThat(saved.getStatus()).isEqualTo("Active");
    }

    @Test
    @DisplayName("downgradeTeamMembers reverts each team member subscription")
    void downgradeTeamMembersRevertsEachMember() {
        Team team = new Team();
        team.setId("team-1");

        User userA = new User();
        userA.setId("user-a");
        User userB = new User();
        userB.setId("user-b");

        UserSubscription subA = new UserSubscription();
        subA.setUser(userA);
        subA.setPlan(proMonthly);
        subA.setPreviousPlan(freePlan);
        subA.setSubscriptionSource(SubscriptionSource.TEAM);

        UserSubscription subB = new UserSubscription();
        subB.setUser(userB);
        subB.setPlan(proMonthly);
        subB.setPreviousPlan(freePlan);
        subB.setSubscriptionSource(SubscriptionSource.TEAM);

        TeamMember memberA = new TeamMember();
        memberA.setUser(userA);
        TeamMember memberB = new TeamMember();
        memberB.setUser(userB);

        when(teamMemberRepository.findByTeamId(team.getId())).thenReturn(List.of(memberA, memberB));
        when(userSubscriptionRepository.findByUserId("user-a")).thenReturn(Optional.of(subA));
        when(userSubscriptionRepository.findByUserId("user-b")).thenReturn(Optional.of(subB));
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        syncService.downgradeTeamMembers(team);

        verify(userSubscriptionRepository, times(2)).save(any(UserSubscription.class));
    }
}

