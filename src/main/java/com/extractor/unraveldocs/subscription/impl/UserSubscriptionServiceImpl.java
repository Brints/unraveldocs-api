package com.extractor.unraveldocs.subscription.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.dto.response.UserSubscriptionDetailsDto;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.subscription.service.UserSubscriptionService;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of UserSubscriptionService.
 * Provides user-facing subscription operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSubscriptionServiceImpl implements UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final NotificationService notificationService;
    private final UserEmailTemplateService emailTemplateService;
    private final SanitizeLogging sanitizer;

    @Override
    @Transactional(readOnly = true)
    public UserSubscriptionDetailsDto getUserSubscriptionDetails(User user) {
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUserIdWithPlan(user.getId());

        if (subscriptionOpt.isEmpty()) {
            // Return a default "no subscription" response
            return UserSubscriptionDetailsDto.builder()
                    .status("none")
                    .planName("Free")
                    .planDisplayName("Free Plan")
                    .isOnTrial(false)
                    .hasUsedTrial(false)
                    .autoRenew(false)
                    .build();
        }

        UserSubscription subscription = subscriptionOpt.get();
        SubscriptionPlan plan = subscription.getPlan();

        // Determine if user is on trial
        boolean isOnTrial = isUserOnTrial(subscription);
        Integer trialDaysRemaining = calculateTrialDaysRemaining(subscription);

        // Format billing interval
        String billingInterval = formatBillingInterval(plan);

        return UserSubscriptionDetailsDto.builder()
                // Subscription identity
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus())

                // Plan information
                .planId(plan.getId())
                .planName(plan.getName().name())
                .planDisplayName(plan.getName().getPlanName())
                .planPrice(plan.getPrice())
                .currency(plan.getCurrency() != null ? plan.getCurrency().name() : "USD")
                .billingInterval(billingInterval)

                // Billing period
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .autoRenew(subscription.isAutoRenew())

                // Trial information
                .isOnTrial(isOnTrial)
                .trialEndsAt(subscription.getTrialEndsAt())
                .hasUsedTrial(subscription.isHasUsedTrial())
                .trialDaysRemaining(trialDaysRemaining)

                // Usage limits
                .storageLimit(plan.getStorageLimit())
                .storageUsed(subscription.getStorageUsed())
                .documentUploadLimit(plan.getDocumentUploadLimit())
                .documentsUploaded(subscription.getMonthlyDocumentsUploaded())
                .ocrPageLimit(plan.getOcrPageLimit())
                .ocrPagesUsed(subscription.getOcrPagesUsed())

                // Payment gateway info
                .paymentGatewaySubscriptionId(subscription.getPaymentGatewaySubscriptionId())

                // Metadata
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    /**
     * Check if user is currently in trial period.
     */
    private boolean isUserOnTrial(UserSubscription subscription) {
        if (subscription.getTrialEndsAt() == null) {
            return false;
        }
        return OffsetDateTime.now().isBefore(subscription.getTrialEndsAt());
    }

    /**
     * Calculate remaining trial days.
     */
    private Integer calculateTrialDaysRemaining(UserSubscription subscription) {
        if (subscription.getTrialEndsAt() == null) {
            return null;
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isAfter(subscription.getTrialEndsAt())) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(now, subscription.getTrialEndsAt());
    }

    @Override
    @Transactional
    public void activateTrial(User user, String planId) {
        // 1. Validate plan exists and supports trial
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (plan.getTrialDays() == null || plan.getTrialDays() <= 0) {
            throw new IllegalArgumentException("This plan does not support trials");
        }

        // 2. Get user subscription
        UserSubscription subscription = userSubscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSubscription newSub = new UserSubscription();
                    newSub.setUser(user);
                    return newSub;
                });

        // 3. Verify trial eligibility
        if (subscription.isHasUsedTrial()) {
            throw new BadRequestException("You have already used your free trial");
        }

        if ("active".equalsIgnoreCase(subscription.getStatus()) &&
                !SubscriptionPlans.FREE.equals(subscription.getPlan().getName())) {
            throw new BadRequestException("You already have an active paid subscription");
        }

        // 4. Activate trial
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime trialEnd = now.plusDays(plan.getTrialDays());

        subscription.setPlan(plan);
        subscription.setStatus("TRIAL");
        subscription.setTrialEndsAt(trialEnd);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(trialEnd);
        subscription.setHasUsedTrial(true);
        subscription.setAutoRenew(false);

        // Reset usage counters for the trial period
        subscription.setStorageUsed(0L);
        subscription.setOcrPagesUsed(0);
        subscription.setMonthlyDocumentsUploaded(0);

        userSubscriptionRepository.save(subscription);

        // Send notifications
        try {
            String expiryDate = trialEnd
                    .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG));

            // Push Notification
            notificationService.sendToUser(
                    user.getId(),
                    com.extractor.unraveldocs.pushnotification.datamodel.NotificationType.TRIAL_ACTIVATED,
                    "Free Trial Activated",
                    "You have successfully started your free trial for the " + plan.getName().getPlanName() + " plan.",
                    Map.of(
                            "planName", plan.getName().getPlanName(),
                            "expiryDate", expiryDate));

            // Email Notification
            emailTemplateService.sendTrialActivatedEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    plan.getName().getPlanName(),
                    expiryDate);
        } catch (Exception e) {
            log.error(
                    "Failed to send trial activation notifications for user {}: {}",
                    sanitizer.sanitizeLogging(user.getId()),
                    e.getMessage());
        }
    }

    @Override
    @Transactional
    public void checkAndExpireTrials() {
        OffsetDateTime now = OffsetDateTime.now();

        // Find subscriptions in TRIAL status that have passed their end date
        java.util.List<UserSubscription> expiredTrials = userSubscriptionRepository
                .findByTrialEndsAtBetweenAndStatusEquals(
                        now.minusYears(1), // Sanity check lower bound
                        now,
                        "TRIAL");

        if (expiredTrials.isEmpty()) {
            return;
        }

        log.info(
                "Found {} expired trials to process",
                sanitizer.sanitizeLoggingInteger(expiredTrials.size()));

        // Get Free plan for reset
        SubscriptionPlan freePlan = subscriptionPlanRepository.findByName(SubscriptionPlans.FREE)
                .orElseThrow(() -> new NotFoundException("Free plan not found in database"));

        for (UserSubscription subscription : expiredTrials) {
            try {
                expireTrial(subscription, freePlan);
            } catch (Exception e) {
                log.error(
                        "Failed to expire trial for subscription {}: {}",
                        sanitizer.sanitizeLogging(subscription.getId()),
                        e.getMessage());
            }
        }
    }

    private void expireTrial(UserSubscription subscription, SubscriptionPlan freePlan) {
        log.info("Expiring trial for user {}", subscription.getUser().getId());

        subscription.setPlan(freePlan);
        subscription.setStatus("ACTIVE");
        subscription.setTrialEndsAt(null);
        subscription.setCurrentPeriodStart(OffsetDateTime.now());
        subscription.setCurrentPeriodEnd(null);

        // Reset auto-renew
        subscription.setAutoRenew(false);

        userSubscriptionRepository.save(subscription);

        // Send notifications
        try {
            String planName = subscription.getPlan().getName().getPlanName();

            // Push Notification
            notificationService.sendToUser(
                    subscription.getUser().getId(),
                    NotificationType.TRIAL_EXPIRED,
                    "Trial Expired",
                    "Your free trial has expired. You have been reverted to the Free plan.",
                    Map.of());

            // Email Notification
            emailTemplateService.sendTrialExpiredEmail(
                    subscription.getUser().getEmail(),
                    subscription.getUser().getFirstName(),
                    subscription.getUser().getLastName(),
                    "Premium"
            );
        } catch (Exception e) {
            log.error(
                    "Failed to send trial expiration notifications for user {}: {}",
                    sanitizer.sanitizeLogging(subscription.getUser().getId()),
                    e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateSubscriptionEligibility(User user) {
        userSubscriptionRepository.findByUserIdWithPlan(user.getId())
                .ifPresent(subscription -> {
                    // Check if subscription is active and not the Free plan
                    if ("active".equalsIgnoreCase(subscription.getStatus()) &&
                            !SubscriptionPlans.FREE.equals(subscription.getPlan().getName())) {
                        throw new BadRequestException(
                                "You currently have an active subscription. Please wait for it to expire or cancel it before purchasing a new one.");
                    }
                });
    }

    /**
     * Format billing interval as human-readable string.
     */
    private String formatBillingInterval(SubscriptionPlan plan) {
        BillingIntervalUnit intervalUnit = plan.getBillingIntervalUnit();
        int intervalValue = plan.getBillingIntervalValue();

        if (intervalValue == 1) {
            return switch (intervalUnit) {
                case WEEK -> "weekly";
                case MONTH -> "monthly";
                case YEAR -> "yearly";
            };
        } else {
            return intervalValue + " " + intervalUnit.name().toLowerCase() + "s";
        }
    }
}
