package com.extractor.unraveldocs.subscription.impl;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionStatus;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignSubscriptionService {
    private final SubscriptionPlanRepository planRepository;

    public UserSubscription assignDefaultSubscription(User user) {
        SubscriptionPlans planName = determinePlanNameForRole(user.getRole());

        Optional<SubscriptionPlan> planOptional = planRepository.findByName(planName);

        if (planOptional.isEmpty()) {
            log.warn("Default subscription plan '{}' not found for user {}. " +
                    "Please ensure plans are created by an admin.", planName, user.getEmail());
            return null;
        }

        SubscriptionPlan plan = planOptional.get();
        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE.getStatusName());
        subscription.setHasUsedTrial(false);

        return subscription;
    }

    private SubscriptionPlans determinePlanNameForRole(Role role) {
        return switch (role) {
            case Role.SUPER_ADMIN, Role.ADMIN -> SubscriptionPlans.BUSINESS_YEARLY;
            case Role.MODERATOR -> SubscriptionPlans.PRO_YEARLY;
            default -> SubscriptionPlans.FREE;
        };
    }
}