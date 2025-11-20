package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.UserData;

public interface GetUserProfileByAdminService {
    UnravelDocsResponse<UserData> getUserProfileByAdmin(String userId);
}
