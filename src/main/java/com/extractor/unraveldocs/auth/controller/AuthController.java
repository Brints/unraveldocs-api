package com.extractor.unraveldocs.auth.controller;

import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.auth.dto.LoginResult;
import com.extractor.unraveldocs.auth.dto.RefreshLoginData;
import com.extractor.unraveldocs.auth.dto.RefreshResult;
import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.*;
import com.extractor.unraveldocs.auth.service.AuthService;
import com.extractor.unraveldocs.security.JwtTokenProvider;
import com.extractor.unraveldocs.user.dto.response.GeneratePasswordResponse;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.utils.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {
        private final AuthService authService;
        private final JwtTokenProvider jwtTokenProvider;

        @Value("${app.cookie-secure:true}")
        private boolean cookieSecure;

        /**
         * Generate a strong password based on the provided criteria.
         *
         * @param password The password generation request containing length and
         *                 excluded characters.
         * @return ResponseEntity containing the generated password.
         */
        @PostMapping("/generate-password")
        @Operation(summary = "Generate a Strong Password.", description = "Generate a strong password based on the provided criteria.", responses = {
                        @ApiResponse(responseCode = "200", description = "Password generated successfully", content = @Content(schema = @Schema(implementation = GeneratePasswordResponse.class)))
        })
        public ResponseEntity<?> generatePassword(
                        @Valid @RequestBody GeneratePasswordDto password) {
                return ResponseEntity.status(HttpStatus.OK).body(authService.generatePassword(password));
        }

        /**
         * Register a new user with optional profile picture.
         *
         * @param request The sign-up request containing user details.
         * @return ResponseEntity indicating the result of the operation.
         */
        @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Register a new user", description = "Register a new user.", responses = {
                        @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = UnravelDocsResponse.class)))
        })
        public ResponseEntity<UnravelDocsResponse<SignupData>> register(
                        @Valid @RequestBody SignupRequestDto request) {
                return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(request));
        }

        /**
         * Login a registered user. The access token is returned in the JSON response
         * body, while the refresh token is set as an HttpOnly cookie.
         *
         * @param request  The login request containing email and password.
         * @param response The HTTP response used to set the refresh token cookie.
         * @return ResponseEntity containing the access token data.
         */
        @PostMapping("/login")
        @Operation(summary = "Login a registered user", description = "Login a registered user. Returns access token in body; refresh token is set as HttpOnly cookie.", responses = {
                        @ApiResponse(responseCode = "200", description = "User logged in successfully", content = @Content(schema = @Schema(implementation = LoginData.class)))
        })
        public ResponseEntity<UnravelDocsResponse<LoginData>> login(
                        @Valid @RequestBody LoginRequestDto request,
                        HttpServletResponse response) {
                LoginResult result = authService.loginUser(request);

                // Set the refresh token as an HttpOnly cookie
                long maxAgeSeconds = jwtTokenProvider.getRefreshExpirationInMs() / 1000;
                CookieUtil.addRefreshTokenCookie(response, result.refreshToken(), maxAgeSeconds, cookieSecure);

                return ResponseEntity.status(HttpStatus.OK).body(result.response());
        }

        /**
         * Verify the user's email address using a token.
         *
         * @param request The email verification request containing email and token.
         * @return ResponseEntity indicating the result of the operation.
         */
        @PostMapping(value = "/verify-email", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Verify user email")
        public ResponseEntity<UnravelDocsResponse<Void>> verifyEmail(
                        @Valid @RequestBody EmailVerificationRequestDto request) {

                UnravelDocsResponse<Void> verifyResponse = authService.verifyEmail(request.email(), request.token());
                return ResponseEntity.status(HttpStatus.OK).body(verifyResponse);
        }

        /**
         * Resend verification email to the user.
         *
         * @param request The email address of the user to resend the verification email
         *                to.
         * @return ResponseEntity indicating the result of the operation.
         */
        @PostMapping("/resend-verification-email")
        @Operation(summary = "Resend verification email")
        public ResponseEntity<UnravelDocsResponse<Void>> resendVerificationEmail(
                        @Valid @RequestBody ResendEmailVerificationDto request) {

                UnravelDocsResponse<Void> resendResponse = authService.resendEmailVerification(request);
                return ResponseEntity.status(HttpStatus.OK).body(resendResponse);
        }

        /**
         * Refresh the user's authentication token. The refresh token is read from the
         * HttpOnly cookie (not from the request body). A new refresh token cookie is
         * set.
         *
         * @param request  The HTTP request containing the refresh token cookie.
         * @param response The HTTP response used to set the new refresh token cookie.
         * @return ResponseEntity containing the refreshed access token data.
         */
        @PostMapping("/refresh-token")
        @Operation(summary = "Refresh authentication token", description = "Obtain a new access token using the refresh token from the HttpOnly cookie.", responses = {
                        @ApiResponse(responseCode = "200", description = "Access token refreshed successfully", content = @Content(schema = @Schema(implementation = UnravelDocsResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
        })
        public ResponseEntity<UnravelDocsResponse<RefreshLoginData>> refreshToken(
                        HttpServletRequest request,
                        HttpServletResponse response) {
                // Read refresh token from HttpOnly cookie
                String refreshToken = CookieUtil.extractRefreshTokenFromCookie(request);
                RefreshResult result = authService.refreshToken(refreshToken);

                // Set the new refresh token as an HttpOnly cookie
                long maxAgeSeconds = jwtTokenProvider.getRefreshExpirationInMs() / 1000;
                CookieUtil.addRefreshTokenCookie(response, result.refreshToken(), maxAgeSeconds, cookieSecure);

                return ResponseEntity.status(HttpStatus.OK).body(result.response());
        }

        /**
         * Logout the user by invalidating the current session.
         * Invalidates both the access token and all associated refresh tokens.
         * Clears the refresh token cookie.
         *
         * @param request  The HTTP request containing the user's session information.
         * @param response The HTTP response used to clear the refresh token cookie.
         * @return ResponseEntity indicating the result of the operation.
         */
        @PostMapping("/logout")
        @Operation(summary = "Logout user", description = "Invalidates the current user's access token and all refresh tokens. Clears the refresh token cookie.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "204", description = "Logged out successfully")
        })
        public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
                authService.logout(request);
                CookieUtil.clearRefreshTokenCookie(response, cookieSecure);
                return ResponseEntity.noContent().build();
        }

        /**
         * Logout the user from all devices by invalidating all sessions.
         * Invalidates the current access token and all refresh tokens for the user.
         * Clears the refresh token cookie.
         *
         * @param request  The HTTP request containing the user's session information.
         * @param response The HTTP response used to clear the refresh token cookie.
         * @return ResponseEntity indicating the result of the operation.
         */
        @PostMapping("/logout-all")
        @Operation(summary = "Logout from all devices", description = "Invalidates all active sessions for the user across all devices. Clears the refresh token cookie.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "204", description = "Logged out from all devices successfully")
        })
        public ResponseEntity<Void> logoutAllDevices(HttpServletRequest request, HttpServletResponse response) {
                authService.logoutAllDevices(request);
                CookieUtil.clearRefreshTokenCookie(response, cookieSecure);
                return ResponseEntity.noContent().build();
        }
}
