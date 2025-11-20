package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface GetAllUsersService {
    UnravelDocsResponse<UserListData> getAllUsers(UserFilterDto request);
}
