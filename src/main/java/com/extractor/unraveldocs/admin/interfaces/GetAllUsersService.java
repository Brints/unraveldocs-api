package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;

public interface GetAllUsersService {
    UnravelDocsDataResponse<UserListData> getAllUsers(UserFilterDto request);
}
