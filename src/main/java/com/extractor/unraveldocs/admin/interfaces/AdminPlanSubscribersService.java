package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminPlanSubscribersService {

    /**
     * Retrieves a paginated list of users subscribed to a specific plan.
     */
    UnravelDocsResponse<UserListData> getPlanSubscribers(String planId, int page, int size);
}
