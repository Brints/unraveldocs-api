package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.request.AdjustSubscriptionDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminSubscriptionActionService {
    UnravelDocsResponse<String> adjustUserSubscription(String userId, AdjustSubscriptionDto request);
    UnravelDocsResponse<String> resetUserQuotas(String userId);
}
