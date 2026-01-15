package com.extractor.unraveldocs.subscription.service;

import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Service for checking subscription-based feature access.
 * Used to restrict premium features to specific subscription tiers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionFeatureService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    /**
     * Premium features that require Starter+ subscription.
     */
    public enum Feature {
        DOCUMENT_MOVE,
        DOCUMENT_ENCRYPTION,
        ADVANCED_SEARCH,
        PRIORITY_OCR
    }

    /**
     * Subscription tiers that have access to premium features.
     */
    private static final Set<SubscriptionPlans> PREMIUM_TIERS = Set.of(
            SubscriptionPlans.STARTER_MONTHLY,
            SubscriptionPlans.STARTER_YEARLY,
            SubscriptionPlans.PRO_MONTHLY,
            SubscriptionPlans.PRO_YEARLY,
            SubscriptionPlans.BUSINESS_MONTHLY,
            SubscriptionPlans.BUSINESS_YEARLY);

    /**
     * Checks if a user has access to a premium feature based on their subscription.
     *
     * @param userId  The user's ID
     * @param feature The premium feature to check
     * @return true if the user has access
     */
    public boolean hasFeatureAccess(String userId, Feature feature) {
        Optional<UserSubscription> subscription = userSubscriptionRepository.findByUserId(userId);

        if (subscription.isEmpty()) {
            log.debug("User {} has no subscription, denying access to {}", userId, feature);
            return false;
        }

        UserSubscription userSub = subscription.get();
        if (userSub.getPlan() == null) {
            log.debug("User {} subscription has no plan, denying access to {}", userId, feature);
            return false;
        }

        SubscriptionPlans planName = userSub.getPlan().getName();
        boolean hasAccess = PREMIUM_TIERS.contains(planName);

        log.debug("User {} with plan {} {} access to {}",
                userId, planName, hasAccess ? "has" : "does not have", feature);

        return hasAccess;
    }

    /**
     * Requires a user to have access to a premium feature, throws
     * ForbiddenException if not.
     *
     * @param userId  The user's ID
     * @param feature The premium feature required
     * @throws ForbiddenException if the user doesn't have access
     */
    public void requireFeatureAccess(String userId, Feature feature) {
        if (!hasFeatureAccess(userId, feature)) {
            throw new ForbiddenException(
                    String.format("This feature requires a Starter or higher subscription. " +
                            "Please upgrade your plan to access %s.", feature.name().toLowerCase().replace("_", " ")));
        }
    }

    /**
     * Checks if a user has any paid subscription (not FREE).
     *
     * @param userId The user's ID
     * @return true if user has a paid subscription
     */
    public boolean hasPaidSubscription(String userId) {
        Optional<UserSubscription> subscription = userSubscriptionRepository.findByUserId(userId);

        if (subscription.isEmpty()) {
            return false;
        }

        UserSubscription userSub = subscription.get();
        if (userSub.getPlan() == null) {
            return false;
        }

        return userSub.getPlan().getName() != SubscriptionPlans.FREE;
    }
}
