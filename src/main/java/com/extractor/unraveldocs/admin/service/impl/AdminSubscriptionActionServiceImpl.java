package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.AdjustSubscriptionDto;
import com.extractor.unraveldocs.admin.interfaces.AdminSubscriptionActionService;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSubscriptionActionServiceImpl implements AdminSubscriptionActionService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    @Transactional
    public UnravelDocsResponse<String> adjustUserSubscription(String userId, AdjustSubscriptionDto request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        UserSubscription subscription = userSubscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Subscription not found for user: " + userId));

        SubscriptionPlan newPlan = subscriptionPlanRepository.findByName(request.getPlan())
                .orElseThrow(() -> new NotFoundException("Plan not found: " + request.getPlan()));

        subscription.setPreviousPlan(subscription.getPlan());
        subscription.setPlan(newPlan);
        subscription.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : false);
        
        // Adjust the renewal/end dates based on interval if needed
        OffsetDateTime now = OffsetDateTime.now();
        subscription.setCurrentPeriodStart(now);
        
        long daysToAdd = 30L;
        if (request.getBillingIntervalUnit() != null && request.getBillingIntervalValue() != null) {
            switch (request.getBillingIntervalUnit().name().toUpperCase()) {
                case "MONTH":
                case "MONTHS":
                    daysToAdd = request.getBillingIntervalValue() * 30L;
                    break;
                case "YEAR":
                case "YEARS":
                    daysToAdd = request.getBillingIntervalValue() * 365L;
                    break;
                case "DAY":
                case "DAYS":
                    daysToAdd = request.getBillingIntervalValue();
                    break;
            }
        }
        subscription.setCurrentPeriodEnd(now.plusDays(daysToAdd));
        subscription.setQuotaResetDate(now.plusDays(daysToAdd));

        userSubscriptionRepository.save(subscription);

        log.info("Admin adjusted subscription for user {}. New plan: {}. Source: {}", 
                userId, request.getPlan(), request.getSource());

        return new UnravelDocsResponse<>(200, "success", "User subscription successfully adjusted to " + request.getPlan(), null);
    }

    @Override
    @Transactional
    public UnravelDocsResponse<String> resetUserQuotas(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        UserSubscription subscription = userSubscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Subscription not found for user: " + userId));

        subscription.setOcrPagesUsed(0);
        subscription.setMonthlyDocumentsUploaded(0);
        subscription.setAiOperationsUsed(0);
        subscription.setQuotaResetDate(OffsetDateTime.now().plusDays(30));

        userSubscriptionRepository.save(subscription);

        log.info("Admin reset quotas for user {}", userId);
        return new UnravelDocsResponse<>(200, "success", "User usage quotas have been reset for the current billing cycle", null);
    }
}
