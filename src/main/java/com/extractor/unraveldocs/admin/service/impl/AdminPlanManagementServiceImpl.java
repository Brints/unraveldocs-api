package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.ActionReasonDto;
import com.extractor.unraveldocs.admin.dto.request.UpdatePlanLimitsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminPlanManagementService;
import com.extractor.unraveldocs.brokers.kafka.events.EventPublisherService;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlanManagementServiceImpl implements AdminPlanManagementService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final EventPublisherService eventPublisherService; // Can reuse for audit logs if needed, omitted exact event for brevity though

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<List<SubscriptionPlan>> getAllPlans() {
        log.info("Fetching all subscription plans for admin dashboard");
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        return new UnravelDocsResponse<>(200, "success", "Plans retrieved successfully", plans);
    }

    @Override
    @Transactional
    public UnravelDocsResponse<String> updatePlanLimits(String planId, UpdatePlanLimitsDto request) {
        log.info("Updating limits for subscription plan ID: {}", planId);

        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found with ID: " + planId));

        plan.setDocumentUploadLimit(request.getDocumentUploadLimit());
        plan.setOcrPageLimit(request.getOcrPageLimit());
        plan.setStorageLimit(request.getStorageLimit());
        plan.setAiOperationsLimit(request.getAiOperationsLimit());
        plan.setPrice(request.getPrice());
        plan.setTrialDays(request.getTrialDays());
        
        // Let Hibernate manage updatedAt automatically via @UpdateTimestamp
        subscriptionPlanRepository.save(plan);

        log.info("Successfully updated limits and pricing for plan: {}", plan.getName());
        return new UnravelDocsResponse<>(200, "success", "Subscription plan updated successfully", "Limits updated");
    }

    @Override
    @Transactional
    public UnravelDocsResponse<String> togglePlanStatus(String planId, boolean isActive, ActionReasonDto reason) {
        log.info("Toggling isActive={} for subscription plan ID: {}", isActive, planId);

        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found with ID: " + planId));

        if (plan.isActive() == isActive) {
            String state = isActive ? "active" : "inactive";
            return new UnravelDocsResponse<>(200, "success", "Plan is already " + state, "No change made");
        }

        plan.setActive(isActive);
        subscriptionPlanRepository.save(plan);

        log.info("Successfully set plan {} to isActive={} with reason: {}", plan.getName(), isActive, reason.getReason());
        
        String actionStatus = isActive ? "activated" : "deactivated";
        return new UnravelDocsResponse<>(200, "success", "Subscription plan successfully " + actionStatus, null);
    }
}
