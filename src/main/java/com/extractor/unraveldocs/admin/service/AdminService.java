package com.extractor.unraveldocs.admin.service;

import com.extractor.unraveldocs.admin.dto.response.ActiveOtpListData;
import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.admin.dto.request.AdminSignupRequestDto;
import com.extractor.unraveldocs.admin.dto.request.OtpRequestDto;
import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.interfaces.*;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final ChangeUserRoleService changeRoleService;
    private final FetchActiveOtpCodes fetchActiveOtpCodes;
    private final GetAllUsersService getAllUsersService;
    private final GetUserProfileByAdminService getProfileByAdmin;
    private final CreateAdminService createAdminService;
    private final GenerateOtpService generateOtpService;

    public UnravelDocsResponse<AdminData> changeUserRole(ChangeRoleDto request, Authentication authentication) {
        return changeRoleService.changeUserRole(request, authentication);
    }

    public UnravelDocsResponse<UserListData> getAllUsers(UserFilterDto request) {
        return getAllUsersService.getAllUsers(request);
    }

    public UnravelDocsResponse<UserData> getUserProfileByAdmin(String userId) {
        return getProfileByAdmin.getUserProfileByAdmin(userId);
    }

    public UnravelDocsResponse<AdminData> createAdmin(AdminSignupRequestDto request) {
        return createAdminService.createAdminUser(request);
    }

    public UnravelDocsResponse<List<String>> generateOtp(OtpRequestDto request) {
        return generateOtpService.generateOtp(request);
    }

    public UnravelDocsResponse<ActiveOtpListData> fetchActiveOtpCodes(int page, int size) {
        return fetchActiveOtpCodes.fetchActiveOtpCodes(page, size);
    }
}
