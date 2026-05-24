package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.request.ActionReasonDto;
import com.extractor.unraveldocs.admin.dto.request.UpdatePlanLimitsDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;

import java.util.List;

public interface AdminPlanManagementService {

    /**
     * Lists all subscription plans.
     */
    UnravelDocsResponse<List<SubscriptionPlan>> getAllPlans();

    /**
     * Updates limits, price, and trial period for a specific plan.
     */
    UnravelDocsResponse<String> updatePlanLimits(String planId, UpdatePlanLimitsDto request);

    /**
     * Activates or deactivates a specific subscription plan.
     */
    UnravelDocsResponse<String> togglePlanStatus(String planId, boolean isActive, ActionReasonDto reason);
}
