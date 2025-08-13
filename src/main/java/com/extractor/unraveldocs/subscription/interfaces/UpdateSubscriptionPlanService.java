package com.extractor.unraveldocs.subscription.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.response.SubscriptionPlansData;

public interface UpdateSubscriptionPlanService {
    UnravelDocsDataResponse<SubscriptionPlansData> updateSubscriptionPlan(UpdateSubscriptionPlanRequest request, String planId);
}
