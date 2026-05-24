package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.UserData;

import com.extractor.unraveldocs.admin.dto.response.AdminUserDetailDto;

public interface GetUserProfileByAdminService {
    UnravelDocsResponse<AdminUserDetailDto> getUserProfileByAdmin(String userId);
}
