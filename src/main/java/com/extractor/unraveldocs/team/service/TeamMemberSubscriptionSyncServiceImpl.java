package com.extractor.unraveldocs.team.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionSource;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionStatus;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionType;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamMemberSubscriptionSyncServiceImpl implements TeamMemberSubscriptionSyncService {

    private final TeamMemberRepository teamMemberRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final NotificationService notificationService;
    private final SanitizeLogging sanitizer;

    @Override
    @Transactional
    public void downgradeTeamMembers(Team team) {
        for (TeamMember member : teamMemberRepository.findByTeamId(team.getId())) {
            revertMemberSubscription(member.getUser());
        }
    }

    @Override
    @Transactional
    public void upgradeTeamMember(User user, Team team) {
        SubscriptionPlan teamPlan = resolveTeamMappedPlan(team);

        UserSubscription subscription = userSubscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSubscription created = new UserSubscription();
                    created.setUser(user);
                    return created;
                });

        if (subscription.getSubscriptionSource() == SubscriptionSource.TEAM
                && subscription.getPlan() != null
                && subscription.getPlan().getId().equals(teamPlan.getId())) {
            return;
        }

        if (subscription.getSubscriptionSource() != SubscriptionSource.TEAM && subscription.getPlan() != null) {
            subscription.setPreviousPlan(subscription.getPlan());
        }

        subscription.setPlan(teamPlan);
        subscription.setStatus(SubscriptionStatus.ACTIVE.getStatusName());
        subscription.setSubscriptionSource(SubscriptionSource.TEAM);

        userSubscriptionRepository.save(subscription);
        notifySafely(user.getId(), NotificationType.SUBSCRIPTION_UPGRADED,
                "Subscription Updated",
                "Your subscription is now managed by your team.",
                Map.of("subscriptionSource", SubscriptionSource.TEAM.name()));
    }

    @Override
    @Transactional
    public void revertMemberSubscription(User user) {
        UserSubscription subscription = userSubscriptionRepository.findByUserId(user.getId()).orElse(null);
        if (subscription == null || subscription.getSubscriptionSource() != SubscriptionSource.TEAM) {
            return;
        }

        SubscriptionPlan fallbackPlan = subscription.getPreviousPlan() != null
                ? subscription.getPreviousPlan()
                : getFreePlan();

        subscription.setPlan(fallbackPlan);
        subscription.setPreviousPlan(null);
        subscription.setSubscriptionSource(SubscriptionSource.INDIVIDUAL);
        subscription.setStatus(SubscriptionStatus.ACTIVE.getStatusName());

        userSubscriptionRepository.save(subscription);
        notifySafely(user.getId(), NotificationType.SUBSCRIPTION_DOWNGRADED,
                "Subscription Updated",
                "Your team-provided subscription has ended and your personal plan has been restored.",
                Map.of("subscriptionSource", SubscriptionSource.INDIVIDUAL.name()));
    }

    private SubscriptionPlan resolveTeamMappedPlan(Team team) {
        SubscriptionPlans mappedPlan = team.getSubscriptionType() == TeamSubscriptionType.TEAM_ENTERPRISE
                ? SubscriptionPlans.BUSINESS_MONTHLY
                : SubscriptionPlans.PRO_MONTHLY;

        return subscriptionPlanRepository.findByName(mappedPlan)
                .orElseThrow(() -> new NotFoundException("Mapped subscription plan not found: " + mappedPlan));
    }

    private SubscriptionPlan getFreePlan() {
        return subscriptionPlanRepository.findByName(SubscriptionPlans.FREE)
                .orElseThrow(() -> new NotFoundException("Free subscription plan not found"));
    }

    private void notifySafely(String userId, NotificationType type, String title, String body, Map<String, String> data) {
        try {
            notificationService.sendToUser(userId, type, title, body, data);
        } catch (Exception ex) {
            log.warn("Unable to send team subscription sync notification for user {}: {}",
                    sanitizer.sanitizeLogging(userId), ex.getMessage());
        }
    }
}

