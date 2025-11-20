package com.extractor.unraveldocs.subscription.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.response.SubscriptionPlansData;

public interface AddSubscriptionPlansService {
    UnravelDocsResponse<SubscriptionPlansData> createSubscriptionPlan(CreateSubscriptionPlanRequest request);
}
