package com.extractor.unraveldocs.auth.controller;

import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.auth.dto.RefreshLoginData;
import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.*;
import com.extractor.unraveldocs.auth.service.AuthService;
import com.extractor.unraveldocs.user.dto.response.GeneratePasswordResponse;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Generate a strong password based on the provided criteria.
     *
     * @param password The password generation request containing length and excluded characters.
     * @return ResponseEntity containing the generated password.
     */
    @PostMapping("/generate-password")
    @Operation(
            summary = "Generate a Strong Password.",
            description = "Generate a strong password based on the provided criteria.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Password generated successfully",
                            content = @Content(schema = @Schema(implementation = GeneratePasswordResponse.class))
                    )
            }
    )
    public ResponseEntity<?> generatePassword(
            @Valid @RequestBody GeneratePasswordDto password
            ) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.generatePassword(password));
    }

    /**
     * Register a new user with optional profile picture.
     *
     * @param request The sign-up request containing user details.
     * @return ResponseEntity indicating the result of the operation.
     */
    @PostMapping(value = "/signup",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Register a new user",
            description = "Register a new user.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User registered successfully",
                            content = @Content(schema = @Schema(implementation = UnravelDocsDataResponse.class))
                    )
            }
    )
    public ResponseEntity<UnravelDocsDataResponse<SignupData>> register(
            @Valid @RequestBody SignUpRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(request));
    }

    /**
     * Login a registered user.
     *
     * @param request The login request containing email and password.
     * @return ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login a registered user",
            description = "Login a registered user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User logged in successfully",
                            content = @Content(schema = @Schema(implementation = LoginData.class))
                    )
            }
    )
    public ResponseEntity<UnravelDocsDataResponse<LoginData>> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.loginUser(request));
    }

    /**
     * Verify the user's email address using a token.
     *
     * @param email The email address of the user to verify.
     * @param token The verification token sent to the user's email.
     * @return ResponseEntity indicating the result of the operation.
     */
    @GetMapping(value = "/verify-email", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verify user email")
    public ResponseEntity<UnravelDocsDataResponse<Void>> verifyEmail(
            @Schema(description = "Email address of the user to verify", example = "john-doe@test.com")
           @RequestParam String email,

            @Schema(description = "Verification token sent to the user's email", example = "1234567890abcdef")
           @RequestParam String token) {

        UnravelDocsDataResponse<Void> response = authService.verifyEmail(email, token);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Resend verification email to the user.
     *
     * @param request The email address of the user to resend the verification email to.
     * @return ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/resend-verification-email")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<UnravelDocsDataResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody ResendEmailVerificationDto request
            ) {

        UnravelDocsDataResponse<Void> response = authService.resendEmailVerification(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Refresh the user's authentication token.
     *
     * @param request The refresh token request containing the current token.
     * @return ResponseEntity containing the refreshed login data.
     */
    @PostMapping("/refresh-token")
    @Operation(
            summary = "Refresh authentication token",
            description = "Obtain a new access token using a valid refresh token.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Access token refreshed successfully",
                            content = @Content(schema = @Schema(implementation = UnravelDocsDataResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid or expired refresh token"
                    )
            }
    )
    public ResponseEntity<UnravelDocsDataResponse<RefreshLoginData>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.refreshToken(request));
    }

    /**
     * Logout the user by invalidating the current session.
     *
     * @param request The HTTP request containing the user's session information.
     * @return ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Invalidates the current user's access token.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Logged out successfully",
                            content = @Content(schema = @Schema(implementation = UnravelDocsDataResponse.class))
                    )
            }
    )
    public ResponseEntity<UnravelDocsDataResponse<Void>> logout(HttpServletRequest request) {
        UnravelDocsDataResponse<Void> response = authService.logout(request);
        return ResponseEntity.status(HttpStatus.valueOf(response.getStatusCode())).body(response);
    }
}
