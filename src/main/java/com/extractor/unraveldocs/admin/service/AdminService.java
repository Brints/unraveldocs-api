package com.extractor.unraveldocs.admin.service;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.response.ActiveOtpListData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.admin.dto.request.AdminSignupRequestDto;
import com.extractor.unraveldocs.admin.dto.request.OtpRequestDto;
import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.interfaces.*;
import com.extractor.unraveldocs.admin.dto.response.AdminUserDetailDto;
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
    private final AdminUserActionService adminUserActionService;
    private final AdminSubscriptionActionService adminSubscriptionActionService;
    private final AdminUserStatsService adminUserStatsService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminSubscriptionStatsService adminSubscriptionStatsService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminPlanManagementService adminPlanManagementService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminPlanSubscribersService adminPlanSubscribersService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminPaymentStatsService adminPaymentStatsService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminReceiptListService adminReceiptListService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminReceiptActionsService adminReceiptActionsService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminCouponStatsService adminCouponStatsService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminCreditStatsService adminCreditStatsService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminDocumentStatsService adminDocumentStatsService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminNotificationStatsService adminNotificationStatsService;
    private final com.extractor.unraveldocs.admin.interfaces.AdminSecurityStatsService adminSecurityStatsService;

    public UnravelDocsResponse<AdminData> changeUserRole(ChangeRoleDto request, Authentication authentication) {
        return changeRoleService.changeUserRole(request, authentication);
    }

    public UnravelDocsResponse<UserListData> getAllUsers(UserFilterDto request) {
        return getAllUsersService.getAllUsers(request);
    }

    public com.extractor.unraveldocs.admin.dto.response.CouponStatsDto getCouponStats() {
        return adminCouponStatsService.getCouponStats().getData();
    }

    public com.extractor.unraveldocs.admin.dto.response.CreditStatsDto getCreditStats() {
        return adminCreditStatsService.getCreditStats().getData();
    }

    public com.extractor.unraveldocs.admin.dto.response.DocumentStatsDto getDocumentStats() {
        return adminDocumentStatsService.getDocumentStats().getData();
    }

    public com.extractor.unraveldocs.admin.dto.response.NotificationStatsDto getNotificationStats() {
        return adminNotificationStatsService.getNotificationStats().getData();
    }

    public com.extractor.unraveldocs.admin.dto.response.SecurityStatsDto getSecurityStats() {
        return adminSecurityStatsService.getSecurityStats().getData();
    }

    public UnravelDocsResponse<AdminUserDetailDto> getUserProfileByAdmin(String userId) {
        return getProfileByAdmin.getUserProfileByAdmin(userId);
    }

    public com.extractor.unraveldocs.admin.dto.response.UserStatsDto getUserStatistics() {
        return adminUserStatsService.getUserStatistics();
    }

    public com.extractor.unraveldocs.admin.dto.response.SubscriptionStatsDto getSubscriptionStatistics() {
        return adminSubscriptionStatsService.getSubscriptionStatistics();
    }

    public UnravelDocsResponse<List<com.extractor.unraveldocs.subscription.model.SubscriptionPlan>> getAllPlans() {
        return adminPlanManagementService.getAllPlans();
    }

    public UnravelDocsResponse<String> updatePlanLimits(String planId, com.extractor.unraveldocs.admin.dto.request.UpdatePlanLimitsDto request) {
        return adminPlanManagementService.updatePlanLimits(planId, request);
    }

    public UnravelDocsResponse<String> togglePlanStatus(String planId, boolean isActive, com.extractor.unraveldocs.admin.dto.request.ActionReasonDto reason) {
        return adminPlanManagementService.togglePlanStatus(planId, isActive, reason);
    }

    public UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.UserListData> getPlanSubscribers(String planId, int page, int size) {
        return adminPlanSubscribersService.getPlanSubscribers(planId, page, size);
    }

    public UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.PaymentStatsDto> getPaymentStatistics() {
        return adminPaymentStatsService.getPaymentStatistics();
    }

    public UnravelDocsResponse<String> toggleUserStatus(String userId, boolean isActive, com.extractor.unraveldocs.admin.dto.request.ActionReasonDto request) {
        return adminUserActionService.toggleUserStatus(userId, isActive, request);
    }

    public UnravelDocsResponse<String> forceVerifyEmail(String userId) {
        return adminUserActionService.forceVerifyEmail(userId);
    }

    public UnravelDocsResponse<String> unlockUser(String userId) {
        return adminUserActionService.unlockUser(userId);
    }

    public UnravelDocsResponse<String> triggerPasswordReset(String userId) {
        return adminUserActionService.triggerPasswordReset(userId);
    }

    public UnravelDocsResponse<String> softDeleteUser(String userId, com.extractor.unraveldocs.admin.dto.request.ActionReasonDto request) {
        return adminUserActionService.softDeleteUser(userId, request);
    }

    public UnravelDocsResponse<com.extractor.unraveldocs.auth.dto.LoginData> impersonateUser(String userId, com.extractor.unraveldocs.admin.dto.request.ActionReasonDto request) {
        return adminUserActionService.impersonateUser(userId, request);
    }

    public UnravelDocsResponse<String> adjustUserSubscription(String userId, com.extractor.unraveldocs.admin.dto.request.AdjustSubscriptionDto request) {
        return adminSubscriptionActionService.adjustUserSubscription(userId, request);
    }

    public UnravelDocsResponse<String> resetUserQuotas(String userId) {
        return adminSubscriptionActionService.resetUserQuotas(userId);
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

    public UnravelDocsResponse<org.springframework.data.domain.Page<com.extractor.unraveldocs.admin.dto.response.AdminReceiptDto>> getReceipts(com.extractor.unraveldocs.admin.dto.request.ReceiptFilterDto filter, int page, int size) {
        return adminReceiptListService.getReceipts(filter, page, size);
    }

    public UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.AdminReceiptDto> getReceiptDetail(String receiptId) {
        return adminReceiptListService.getReceiptDetail(receiptId);
    }

    public UnravelDocsResponse<String> resendReceiptEmail(String receiptId) {
        return adminReceiptActionsService.resendReceiptEmail(receiptId);
    }
}
