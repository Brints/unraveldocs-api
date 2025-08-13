package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.dto.UserData;

public interface GetUserProfileByAdminService {
    UnravelDocsDataResponse<UserData> getUserProfileByAdmin(String userId);
}
