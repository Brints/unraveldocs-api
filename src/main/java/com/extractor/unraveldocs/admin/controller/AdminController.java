package com.extractor.unraveldocs.admin.controller;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.admin.dto.request.CreateAdminRequestDto;
import com.extractor.unraveldocs.admin.dto.request.OtpRequestDto;
import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.service.AdminService;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
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
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {
    private final AdminService adminService;

    /**
     * Create an admin user if none exists.
     * @param request The sign-up request containing user details.
     *
     * @return ResponseEntity containing the created admin user data.
     */
    @Operation(
            summary = "Create Admin User",
            description = "Allows users to register as an admin to manage the application.")
    @PostMapping("/create-admin")
    public ResponseEntity<UnravelDocsResponse<AdminData>> createAdmin(
            @Valid @RequestBody CreateAdminRequestDto request
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
            description = "Allows an admin to change the role of a user to ADMIN or MODERATOR.")
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
     * @param request         The request containing filter and pagination parameters.
     * @param authentication  The current user's authentication details.
     * @return ResponseEntity containing the list of users and pagination details.
     */
    @Operation(
            summary = "Get all users",
            description = "Fetches a paginated list of all users with optional filtering and sorting.")
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
            description = "Fetches the profile of a user by admin.")
    @GetMapping("/{userId}")
    public ResponseEntity<UnravelDocsResponse<UserData>> getUserProfileByAdmin(@PathVariable String userId) {
        UnravelDocsResponse<UserData> response = adminService.getUserProfileByAdmin(userId);
        return ResponseEntity.ok(response);
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
    @PostMapping("/generate-otp")
    public ResponseEntity<UnravelDocsResponse<List<String>>> generateOtp(@Valid @RequestBody OtpRequestDto request) {
        UnravelDocsResponse<List<String>> response = adminService.generateOtp(request);
        return ResponseEntity.ok(response);
    }
}
