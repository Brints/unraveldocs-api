package com.extractor.unraveldocs.subscription.service;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.response.AllSubscriptionPlans;
import com.extractor.unraveldocs.subscription.dto.response.SubscriptionPlansData;
import com.extractor.unraveldocs.subscription.interfaces.AddSubscriptionPlansService;
import com.extractor.unraveldocs.subscription.interfaces.AssignSubscriptionToUsersService;
import com.extractor.unraveldocs.subscription.interfaces.UpdateSubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionPlansService {
    private final AssignSubscriptionToUsersService assignSubscriptionToUsersService;
    private final AddSubscriptionPlansService addSubscriptionPlansService;
    private final UpdateSubscriptionPlanService updateSubscriptionPlanService;

    public UnravelDocsDataResponse<SubscriptionPlansData> createSubscriptionPlan(
            CreateSubscriptionPlanRequest request) {
        return addSubscriptionPlansService.createSubscriptionPlan(request);
    }

    public UnravelDocsDataResponse<SubscriptionPlansData> updateSubscriptionPlan(
            String planId, UpdateSubscriptionPlanRequest request) {
        return updateSubscriptionPlanService.updateSubscriptionPlan(request, planId);
    }

    public UnravelDocsDataResponse<AllSubscriptionPlans> assignSubscriptionsToExistingUsers() {
        return assignSubscriptionToUsersService.assignSubscriptionsToExistingUsers();
    }
}
