package com.extractor.unraveldocs.admin.controller;

import com.extractor.unraveldocs.admin.dto.response.ActiveOtpListData;
import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.admin.dto.request.AdminSignupRequestDto;
import com.extractor.unraveldocs.admin.dto.request.OtpRequestDto;
import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.service.AdminService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.admin.dto.response.AdminUserDetailDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.UserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(
        name = "Admin Management",
        description = "Admin endpoints for user management and other administrative tasks"
)
public class AdminController {
    private final AdminService adminService;

    /**
     * Create an admin user if none exists.
     *
     * @param request The sign-up request containing user details.
     * @return ResponseEntity containing the created admin user data.
     */
    @Operation(
            summary = "Create Admin User",
            description = "Allows users to register as an admin to manage the application.")
    @PostMapping("/signup")
    public ResponseEntity<UnravelDocsResponse<AdminData>> createAdmin(
            @Valid @RequestBody AdminSignupRequestDto request
    ) {
        UnravelDocsResponse<AdminData> response = adminService.createAdmin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Change the role of a user to ADMIN or MODERATOR.
     *
     * @param authenticatedUser The currently authenticated user.
     * @param request           The request containing the user ID and new role.
     * @return ResponseEntity containing the result of the operation.
     */
    @Operation(
            summary = "Change user role to ADMIN or MODERATOR",
            description = "Allows an admin or super admin to change the role of a user to ADMIN or MODERATOR.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/change-role")
    public ResponseEntity<UnravelDocsResponse<AdminData>> changeUserRole(
            Authentication authenticatedUser,
            @RequestBody ChangeRoleDto request
    ) {
        if (authenticatedUser == null) {
            throw new ForbiddenException("You must be logged in to change user roles");
        }

        UnravelDocsResponse<AdminData> response = adminService.changeUserRole(request, authenticatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a paginated list of all users with optional filtering and sorting.
     *
     * @param request        The request containing filter and pagination parameters.
     * @param authentication The current user's authentication details.
     * @return ResponseEntity containing the list of users and pagination details.
     */
    @Operation(
            summary = "Get all users",
            description = "Fetches a paginated list of all users with optional filtering and sorting.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR') or hasRole('SUPER_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<UnravelDocsResponse<UserListData>> getAllUsers(
            @Valid @ModelAttribute UserFilterDto request,
            Authentication authentication
    ) {

        if (authentication == null) {
            throw new ForbiddenException("You must be logged in to view users");
        }

        UnravelDocsResponse<UserListData> response = adminService.getAllUsers(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get the profile of a user by admin.
     *
     * @param userId The ID of the user whose profile is to be fetched.
     * @return ResponseEntity containing the user's profile data.
     */
    @Operation(
            summary = "Get user profile by admin",
            description = "Fetches the full enriched profile of a user by admin or super admin.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<UnravelDocsResponse<AdminUserDetailDto>> getUserProfileByAdmin(@PathVariable String userId) {
        UnravelDocsResponse<AdminUserDetailDto> response = adminService.getUserProfileByAdmin(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Activate User", description = "Activates a deactivated user.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<UnravelDocsResponse<String>> activateUser(
            @PathVariable String userId,
            @Valid @RequestBody com.extractor.unraveldocs.admin.dto.request.ActionReasonDto request) {
        return ResponseEntity.ok(adminService.toggleUserStatus(userId, true, request));
    }

    @Operation(summary = "Deactivate User", description = "Deactivates an active user.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<UnravelDocsResponse<String>> deactivateUser(
            @PathVariable String userId,
            @Valid @RequestBody com.extractor.unraveldocs.admin.dto.request.ActionReasonDto request) {
        return ResponseEntity.ok(adminService.toggleUserStatus(userId, false, request));
    }

    @Operation(summary = "Force Verify User Email", description = "Manually verifies a user's email address.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/users/{userId}/force-verify")
    public ResponseEntity<UnravelDocsResponse<String>> forceVerifyEmail(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.forceVerifyEmail(userId));
    }

    @Operation(summary = "Unlock User", description = "Unlocks a user account locked due to too many failed logins.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<UnravelDocsResponse<String>> unlockUser(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.unlockUser(userId));
    }

    @Operation(summary = "Reset Password", description = "Triggers a password reset token email for the user.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<UnravelDocsResponse<String>> resetUserPassword(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.triggerPasswordReset(userId));
    }

    @Operation(summary = "Soft Delete User", description = "Soft-deletes a user from the platform.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/users/{userId}/soft-delete")
    public ResponseEntity<UnravelDocsResponse<String>> softDeleteUser(
            @PathVariable String userId,
            @Valid @RequestBody com.extractor.unraveldocs.admin.dto.request.ActionReasonDto request) {
        return ResponseEntity.ok(adminService.softDeleteUser(userId, request));
    }

    @Operation(summary = "Impersonate User", description = "Generates a login token to impersonate the user (Requires Super Admin).")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{userId}/impersonate")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.auth.dto.LoginData>> impersonateUser(
            @PathVariable String userId,
            @Valid @RequestBody com.extractor.unraveldocs.admin.dto.request.ActionReasonDto request) {
        return ResponseEntity.ok(adminService.impersonateUser(userId, request));
    }

    @Operation(summary = "Adjust Subscription", description = "Manually adjusts a user's subscription tier.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/users/{userId}/subscription")
    public ResponseEntity<UnravelDocsResponse<String>> adjustSubscription(
            @PathVariable String userId,
            @Valid @RequestBody com.extractor.unraveldocs.admin.dto.request.AdjustSubscriptionDto request) {
        return ResponseEntity.ok(adminService.adjustUserSubscription(userId, request));
    }

    @Operation(summary = "Reset Quotas", description = "Manually resets a user's usage quotas for the current billing cycle.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{userId}/quotas/reset")
    public ResponseEntity<UnravelDocsResponse<String>> resetQuotas(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.resetUserQuotas(userId));
    }

    @Operation(
            summary = "Get User Statistics",
            description = "Returns aggregated user statistics for admin dashboard charts and KPIs.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MODERATOR')")
    @GetMapping("/users/stats")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.UserStatsDto>> getUserStats() {
        com.extractor.unraveldocs.admin.dto.response.UserStatsDto stats = adminService.getUserStatistics();
        return ResponseEntity.ok(new UnravelDocsResponse<>(200, "success", "User statistics retrieved successfully", stats));
    }

    @Operation(
            summary = "Get Subscription Statistics",
            description = "Returns aggregated subscription and revenue stats for admin dashboard.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MODERATOR')")
    @GetMapping("/subscriptions/stats")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.SubscriptionStatsDto>> getSubscriptionStats() {
        com.extractor.unraveldocs.admin.dto.response.SubscriptionStatsDto stats = adminService.getSubscriptionStatistics();
        return ResponseEntity.ok(new UnravelDocsResponse<>(200, "success", "Subscription statistics retrieved successfully", stats));
    }

    @Operation(summary = "List all Subscription Plans", description = "Retrieves all subscription plans with prices and quota limits.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MODERATOR')")
    @GetMapping("/subscriptions/plans")
    public ResponseEntity<UnravelDocsResponse<List<com.extractor.unraveldocs.subscription.model.SubscriptionPlan>>> getAllPlans() {
        return ResponseEntity.ok(adminService.getAllPlans());
    }

    @Operation(summary = "Update Plan Limits", description = "Updates limits, trial days, and pricing for a specific subscription plan.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/subscriptions/plans/{planId}")
    public ResponseEntity<UnravelDocsResponse<String>> updatePlanLimits(
            @PathVariable String planId,
            @Valid @RequestBody com.extractor.unraveldocs.admin.dto.request.UpdatePlanLimitsDto request) {
        return ResponseEntity.ok(adminService.updatePlanLimits(planId, request));
    }

    @Operation(summary = "Toggle Plan Status", description = "Activates or deactivates a subscription plan.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/subscriptions/plans/{planId}/status")
    public ResponseEntity<UnravelDocsResponse<String>> togglePlanStatus(
            @PathVariable String planId,
            @RequestParam boolean isActive,
            @Valid @RequestBody com.extractor.unraveldocs.admin.dto.request.ActionReasonDto request) {
        return ResponseEntity.ok(adminService.togglePlanStatus(planId, isActive, request));
    }

    @Operation(summary = "List Plan Subscribers", description = "Returns a paginated list of users subscribed to a specific plan.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MODERATOR')")
    @GetMapping("/subscriptions/plans/{planId}/subscribers")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.UserListData>> getPlanSubscribers(
            @PathVariable String planId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getPlanSubscribers(planId, page, size));
    }

    /**
     * Generate a One-Time Password (OTP) of specified length.
     *
     * @param request The request containing OTP generation parameters.
     * @return ResponseEntity containing the generated OTP.
     */
    @Operation(
            summary = "Generate OTP",
            description = "Generates a One-Time Password (OTP) of specified length.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/generate-otp")
    public ResponseEntity<UnravelDocsResponse<List<String>>> generateOtp(
            @Valid @RequestBody OtpRequestDto request
    ) {
        UnravelDocsResponse<List<String>> response = adminService.generateOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetch all active OTPs.
     *
     * @return ResponseEntity containing the list of active OTPs.
     */
    @Operation(
            summary = "Fetch Active OTPs",
            description = "Fetches all active One-Time Passwords (OTPs).")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    @GetMapping("/active-otps")
    public ResponseEntity<UnravelDocsResponse<ActiveOtpListData>> fetchActiveOtps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        UnravelDocsResponse<ActiveOtpListData> response = adminService.fetchActiveOtpCodes(page, size);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // COUPON STATISTICS
    // ==========================================

    @Operation(
            summary = "Get Coupon Statistics",
            description = "Retrieves aggregated coupon metrics and analytics.")
    @GetMapping("/coupons/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.CouponStatsDto>> getCouponStats() {
        com.extractor.unraveldocs.admin.dto.response.CouponStatsDto stats = adminService.getCouponStats();
        return ResponseEntity.ok(new UnravelDocsResponse<>(200, "success", "Coupon stats retrieved successfully", stats));
    }

    // ==========================================
    // CREDIT STATISTICS (Phase 6B)
    // ==========================================

    @Operation(
            summary = "Get Credit Statistics",
            description = "Retrieves aggregated credit usage and transaction metrics.")
    @GetMapping("/credits/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.CreditStatsDto>> getCreditStats() {
        com.extractor.unraveldocs.admin.dto.response.CreditStatsDto stats = adminService.getCreditStats();
        return ResponseEntity.ok(new UnravelDocsResponse<>(200, "success", "Credit stats retrieved successfully", stats));
    }

    // ==========================================
    // DOCUMENT STATISTICS (Phase 6C)
    // ==========================================

    @Operation(
            summary = "Get Document Statistics",
            description = "Retrieves aggregated document, file and storage metrics.")
    @GetMapping("/documents/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.DocumentStatsDto>> getDocumentStats() {
        com.extractor.unraveldocs.admin.dto.response.DocumentStatsDto stats = adminService.getDocumentStats();
        return ResponseEntity.ok(new UnravelDocsResponse<>(200, "success", "Document stats retrieved successfully", stats));
    }

    // ==========================================
    // NOTIFICATION STATISTICS (Phase 6D)
    // ==========================================

    @Operation(
            summary = "Get Notification Statistics",
            description = "Retrieves aggregated notification engagement and device metrics.")
    @GetMapping("/notifications/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.NotificationStatsDto>> getNotificationStats() {
        com.extractor.unraveldocs.admin.dto.response.NotificationStatsDto stats = adminService.getNotificationStats();
        return ResponseEntity.ok(new UnravelDocsResponse<>(200, "success", "Notification stats retrieved successfully", stats));
    }

    // ==========================================
    // SECURITY STATISTICS (Phase 6E)
    // ==========================================

    @Operation(
            summary = "Get Security Statistics",
            description = "Retrieves aggregated security metrics including bans and failed logins.")
    @GetMapping("/security/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.SecurityStatsDto>> getSecurityStats() {
        com.extractor.unraveldocs.admin.dto.response.SecurityStatsDto stats = adminService.getSecurityStats();
        return ResponseEntity.ok(new UnravelDocsResponse<>(200, "success", "Security stats retrieved successfully", stats));
    }

    // ==========================================
    // PAYMENT & REVENUE STATISTICS (Phase 4A)
    // ==========================================

    @Operation(
            summary = "Get Payment Statistics",
            description = "Retrieves aggregated payment and revenue metrics.")
    @GetMapping("/payments/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.PaymentStatsDto>> getPaymentStats() {
        return ResponseEntity.ok(adminService.getPaymentStatistics());
    }

    // ==========================================
    // RECEIPT MANAGEMENT (Phase 4B)
    // ==========================================

    @Operation(
            summary = "Get All Receipts",
            description = "Retrieves a paginated list of receipts with optional filtering.")
    @GetMapping("/receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UnravelDocsResponse<org.springframework.data.domain.Page<com.extractor.unraveldocs.admin.dto.response.AdminReceiptDto>>> getReceipts(
            @Valid @ModelAttribute com.extractor.unraveldocs.admin.dto.request.ReceiptFilterDto filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getReceipts(filter, page, size));
    }

    @Operation(
            summary = "Get Receipt Details",
            description = "Retrieves details of a specific receipt.")
    @GetMapping("/receipts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UnravelDocsResponse<com.extractor.unraveldocs.admin.dto.response.AdminReceiptDto>> getReceiptDetail(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(adminService.getReceiptDetail(id));
    }

    @Operation(
            summary = "Resend Receipt Email",
            description = "Resends a payment receipt email to the user.")
    @PostMapping("/receipts/{id}/resend")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UnravelDocsResponse<String>> resendReceiptEmail(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(adminService.resendReceiptEmail(id));
    }
}
