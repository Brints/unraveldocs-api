package com.extractor.unraveldocs.user.interfaces.userimpl;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.UserData;

public interface GetUserProfileService {
    UnravelDocsResponse<UserData> getUserProfileByOwner(String userId);
}
