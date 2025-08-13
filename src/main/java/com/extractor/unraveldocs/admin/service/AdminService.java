package com.extractor.unraveldocs.admin.service;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.interfaces.ChangeUserRoleService;
import com.extractor.unraveldocs.admin.interfaces.GetAllUsersService;
import com.extractor.unraveldocs.admin.interfaces.GetUserProfileByAdminService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.dto.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final ChangeUserRoleService changeRoleService;
    private final GetAllUsersService getAllUsersService;
    private final GetUserProfileByAdminService getProfileByAdmin;

    public UnravelDocsDataResponse<AdminData> changeUserRole(ChangeRoleDto request, Authentication authentication) {
        return changeRoleService.changeUserRole(request, authentication);
    }

    public UnravelDocsDataResponse<UserListData> getAllUsers(UserFilterDto request) {
        return getAllUsersService.getAllUsers(request);
    }

    public UnravelDocsDataResponse<UserData> getUserProfileByAdmin(String userId) {
        return getProfileByAdmin.getUserProfileByAdmin(userId);
    }
}
