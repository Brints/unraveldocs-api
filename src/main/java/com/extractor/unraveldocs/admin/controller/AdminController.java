package com.extractor.unraveldocs.admin.controller;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.admin.dto.request.UserFilterDto;
import com.extractor.unraveldocs.admin.dto.response.UserListData;
import com.extractor.unraveldocs.admin.service.AdminService;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.dto.UserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin endpoints for user management and other administrative tasks")
public class AdminController {
    private final AdminService adminService;

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
    public ResponseEntity<UnravelDocsDataResponse<AdminData>> changeUserRole(Authentication authenticatedUser, @RequestBody ChangeRoleDto request) {
        if (authenticatedUser == null) {
            throw new ForbiddenException("You must be logged in to change user roles");
        }

        UnravelDocsDataResponse<AdminData> response = adminService.changeUserRole(request, authenticatedUser);

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
    public ResponseEntity<UnravelDocsDataResponse<UserListData>> getAllUsers(
            @Valid @ModelAttribute UserFilterDto request,
            Authentication authentication) {

        if (authentication == null) {
            throw new ForbiddenException("You must be logged in to view users");
        }

        UnravelDocsDataResponse<UserListData> response = adminService.getAllUsers(request);

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
    public ResponseEntity<UnravelDocsDataResponse<UserData>> getUserProfileByAdmin(@PathVariable String userId) {
        UnravelDocsDataResponse<UserData> response = adminService.getUserProfileByAdmin(userId);
        return ResponseEntity.ok(response);
    }
}
