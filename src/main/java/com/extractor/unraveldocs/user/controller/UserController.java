package com.extractor.unraveldocs.user.controller;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.TooManyRequestsException;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.dto.request.*;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.interfaces.passwordreset.PasswordResetParams;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.user.service.UserService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Manage user profiles and settings")
public class UserController {
        private final UserService userService;
        private final UserRepository userRepository;

        private final Map<String, Bucket> forgotPasswordBuckets = new ConcurrentHashMap<>();
        private final Map<String, Bucket> resetPasswordBuckets = new ConcurrentHashMap<>();
        private final Map<String, Bucket> userActionBuckets = new ConcurrentHashMap<>();

        private User getAuthenticatedUser(UserDetails authenticatedUser) {
                if (authenticatedUser == null) {
                        throw new ForbiddenException("Please login to perform this action.");
                }
                return userRepository.findByEmail(authenticatedUser.getUsername())
                                .orElseThrow(() -> new ForbiddenException("User not found"));
        }

        private Bucket createForgotPasswordBucket(String key) {
                Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofHours(1)));
                return Bucket.builder().addLimit(limit).build();
        }

        private Bucket createResetPasswordBucket(String key) {
                Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofHours(1)));
                return Bucket.builder().addLimit(limit).build();
        }

        private Bucket createUserActionBucket(String key) {
                Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
                return Bucket.builder().addLimit(limit).build();
        }

        @Operation(summary = "Get current user profile")
        @GetMapping("/me")
        public ResponseEntity<?> getAuthenticatedUserProfile(
                        @AuthenticationPrincipal UserDetails authenticatedUser) {
                User user = getAuthenticatedUser(authenticatedUser);
                return ResponseEntity.ok(userService.getUserProfileByOwner(user.getId()));
        }

        @Operation(summary = "Forgot password")
        @PostMapping("/forgot-password")
        public ResponseEntity<@NonNull UnravelDocsResponse<Void>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordDto request) {
                Bucket bucket = forgotPasswordBuckets.computeIfAbsent(
                                request.email(),
                                this::createForgotPasswordBucket);

                if (!bucket.tryConsume(1)) {
                        throw new TooManyRequestsException(
                                        "You have made too many password reset requests. Please try again later.");
                }
                return ResponseEntity.ok(userService.forgotPassword(request));
        }

        @Operation(summary = "Reset password")
        @PostMapping("/reset-password")
        public ResponseEntity<@NonNull UnravelDocsResponse<Void>> resetPassword(
                        @Valid @RequestBody ResetPasswordDto request) {
                Bucket bucket = resetPasswordBuckets.computeIfAbsent(request.email(), this::createResetPasswordBucket);
                if (!bucket.tryConsume(1)) {
                        throw new TooManyRequestsException(
                                        "You have made too many password reset attempts. Please try again later.");
                }

                PasswordResetParams params = new PasswordResetParams(
                                request.email(),
                                request.token());
                return ResponseEntity.ok(userService.resetPassword(params, request));
        }

        @Operation(summary = "Change password")
        @PostMapping("/change-password")
        public ResponseEntity<@NonNull UnravelDocsResponse<Void>> changePassword(
                        @AuthenticationPrincipal UserDetails authenticatedUser,
                        @Valid @RequestBody ChangePasswordDto changePasswordDto) {
                User user = getAuthenticatedUser(authenticatedUser);
                Bucket bucket = userActionBuckets.computeIfAbsent(user.getId(), this::createUserActionBucket);
                if (!bucket.tryConsume(1)) {
                        throw new TooManyRequestsException(
                                        "You have made too many password change requests. Please try again later.");
                }
                return ResponseEntity.ok(userService.changePassword(changePasswordDto));
        }

        @Operation(summary = "Update user profile", description = "User can update some information in their profile. For profile picture uploads, use the separate /profile/{userId}/upload endpoint.", responses = {
                        @ApiResponse(responseCode = "200", description = "User profile updated successfully", content = @Content(schema = @Schema(implementation = UnravelDocsResponse.class)))
        })
        @PutMapping(value = "/profile/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<@NonNull UnravelDocsResponse<UserData>> updateProfile(
                        @AuthenticationPrincipal UserDetails authenticatedUser,
                        @Valid @RequestBody ProfileUpdateRequestDto request,
                        @PathVariable("userId") String userId) {
                User user = getAuthenticatedUser(authenticatedUser);

                if (request == null) {
                        throw new BadRequestException("Request body cannot be null");
                }

                Bucket bucket = userActionBuckets.computeIfAbsent(user.getId(), this::createUserActionBucket);
                if (!bucket.tryConsume(1)) {
                        throw new TooManyRequestsException(
                                        "You have made too many profile update requests. Please try again later.");
                }

                return ResponseEntity.ok(userService.updateProfile(request, userId));
        }

        @Operation(summary = "Delete user profile")
        @DeleteMapping("/profile/{userId}")
        public ResponseEntity<?> deleteUser(
                        @AuthenticationPrincipal UserDetails authenticatedUser,
                        @PathVariable("userId") String userId) {
                User user = getAuthenticatedUser(authenticatedUser);

                Bucket bucket = userActionBuckets.computeIfAbsent(user.getId(), this::createUserActionBucket);
                if (!bucket.tryConsume(1)) {
                        throw new TooManyRequestsException(
                                        "You have made too many account deletion requests. Please try again later.");
                }
                userService.deleteUser(userId);
                return ResponseEntity.ok("User profile deleted successfully");
        }

        @Operation(summary = "Upload profile picture", description = "User can upload a profile picture.", responses = {
                        @ApiResponse(responseCode = "200", description = "Profile picture uploaded successfully", content = @Content(schema = @Schema(implementation = UnravelDocsResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid file type or empty file", content = @Content(schema = @Schema(implementation = UnravelDocsResponse.class)))
        })
        @PostMapping(value = "/profile/{userId}/upload", consumes = "multipart/form-data")
        public ResponseEntity<@NonNull UnravelDocsResponse<String>> uploadProfilePicture(
                        @AuthenticationPrincipal UserDetails authenticatedUser,
                        @RequestParam("file") @NotNull MultipartFile file,
                        @PathVariable String userId) {
                // Validate the authenticated user
                User user = getAuthenticatedUser(authenticatedUser);

                // Validate the file
                if (file.isEmpty()) {
                        throw new BadRequestException("File cannot be empty");
                }

                // Create a bucket for user actions if it doesn't exist
                Bucket bucket = userActionBuckets.computeIfAbsent(userId, this::createUserActionBucket);
                if (!bucket.tryConsume(1)) {
                        throw new TooManyRequestsException(
                                        "You have made too many profile picture upload requests. Please try again later.");
                }

                return ResponseEntity.ok(userService.uploadProfilePicture(user, file));
        }

        @Operation(summary = "Delete profile picture", description = "User can delete their profile picture.", responses = {
                        @ApiResponse(responseCode = "200", description = "Profile picture deleted successfully", content = @Content(schema = @Schema(implementation = UnravelDocsResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Profile picture not found or already deleted", content = @Content(schema = @Schema(implementation = UnravelDocsResponse.class)))
        })
        @DeleteMapping("/profile/{userId}/delete")
        public ResponseEntity<@NonNull UnravelDocsResponse<Void>> deleteProfilePicture(
                        @AuthenticationPrincipal UserDetails authenticatedUser,
                        @PathVariable("userId") String userId) {
                User user = getAuthenticatedUser(authenticatedUser);

                Bucket bucket = userActionBuckets.computeIfAbsent(userId, this::createUserActionBucket);
                if (!bucket.tryConsume(1)) {
                        throw new TooManyRequestsException(
                                        "You have made too many profile picture deletion requests. Please try again later.");
                }
                userService.deleteProfilePicture(user);

                return ResponseEntity.ok(userService.deleteProfilePicture(user));
        }
}