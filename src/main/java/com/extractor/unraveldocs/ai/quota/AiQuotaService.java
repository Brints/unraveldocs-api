package com.extractor.unraveldocs.ai.quota;

import com.extractor.unraveldocs.credit.service.CreditBalanceService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing AI usage billing with the hybrid model:
 * "Subscription allowance first, credits as overflow."
 *
 * <p>
 * Each subscription tier includes a monthly AI operations allowance.
 * When the allowance is exhausted, credits are deducted from the user's
 * credit balance. If neither is available, the operation is denied.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final CreditBalanceService creditBalanceService;
    private final UserRepository userRepository;
    private final SanitizeLogging sanitizer;

    /**
     * Attempt to consume an AI operation for the given user.
     * <p>
     * Priority: subscription allowance → credit balance → denied.
     *
     * @param userId     The user's ID
     * @param creditCost The credit cost if billing falls through to credits
     * @return AiCostResult indicating whether the operation is allowed and billing
     *         source
     */
    @Transactional
    public AiCostResult consumeAiOperation(String userId, int creditCost) {
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserId(userId);

        // Step 1: Try subscription allowance
        if (subscriptionOpt.isPresent()) {
            UserSubscription subscription = subscriptionOpt.get();
            SubscriptionPlan plan = subscription.getPlan();

            if (plan != null) {
                int limit = plan.getAiOperationsLimit();
                int used = subscription.getAiOperationsUsed() != null
                        ? subscription.getAiOperationsUsed()
                        : 0;

                if (limit < 0 || used < limit) {
                    // Subscription has remaining allowance (or unlimited)
                    subscription.setAiOperationsUsed(used + 1);
                    userSubscriptionRepository.save(subscription);

                    log.debug("AI operation consumed from subscription for user {}. "
                            + "Used: {}/{}",
                            sanitizer.sanitizeLogging(userId),
                            sanitizer.sanitizeLoggingInteger(used + 1),
                            sanitizer.sanitizeLoggingInteger(limit));
                    return AiCostResult.fromSubscription();
                }
            }
        }

        // Step 2: Try credit balance
        if (creditBalanceService.hasEnoughCredits(userId, creditCost)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                creditBalanceService.deductCredits(
                        user,
                        creditCost,
                        "ai-operation",
                        "AI operation (overflow from subscription allowance)");

                log.debug("AI operation charged {} credits.",
                        sanitizer.sanitizeLoggingInteger(creditCost));
                return AiCostResult.fromCredits(creditCost);
            }
        }

        // Step 3: Neither available
        log.info("AI operation denied: no subscription allowance or credits");
        return AiCostResult.denied(
                "You have used all your AI operations for this month and have insufficient credits. "
                        + "Please purchase more credits or upgrade your subscription plan.");
    }

    /**
     * Check if a user has access to premium AI models (GPT-4o, Mistral Large)
     * based on their subscription tier.
     *
     * @param userId The user's ID
     * @return true if user has Pro or Business subscription
     */
    public boolean hasPremiumModelAccess(String userId) {
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserId(userId);

        if (subscriptionOpt.isEmpty()) {
            return false;
        }

        UserSubscription subscription = subscriptionOpt.get();
        SubscriptionPlan plan = subscription.getPlan();

        if (plan == null) {
            return false;
        }

        SubscriptionPlans planName = plan.getName();
        return planName == SubscriptionPlans.PRO_MONTHLY
                || planName == SubscriptionPlans.PRO_YEARLY
                || planName == SubscriptionPlans.BUSINESS_MONTHLY
                || planName == SubscriptionPlans.BUSINESS_YEARLY;
    }
}
