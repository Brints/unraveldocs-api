package com.extractor.unraveldocs.subscription.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.subscription.dto.response.AllSubscriptionPlans;

public interface AssignSubscriptionToUsersService {
    UnravelDocsDataResponse<AllSubscriptionPlans> assignSubscriptionsToExistingUsers();
}
