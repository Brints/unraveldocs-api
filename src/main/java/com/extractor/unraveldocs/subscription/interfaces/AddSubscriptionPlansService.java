package com.extractor.unraveldocs.subscription.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.response.SubscriptionPlansData;

public interface AddSubscriptionPlansService {
    UnravelDocsDataResponse<SubscriptionPlansData> createSubscriptionPlan(CreateSubscriptionPlanRequest request);
}
