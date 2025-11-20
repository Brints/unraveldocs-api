package com.extractor.unraveldocs.subscription.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.dto.response.AllSubscriptionPlans;

public interface AssignSubscriptionToUsersService {
    UnravelDocsResponse<AllSubscriptionPlans> assignSubscriptionsToExistingUsers();
}
