package com.extractor.unraveldocs.user.interfaces.userimpl;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.dto.UserData;

public interface GetUserProfileService {
    UnravelDocsDataResponse<UserData> getUserProfileByOwner(String userId);
}
