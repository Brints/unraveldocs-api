package com.extractor.unraveldocs.auth.impl;

import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.auth.dto.LoginResult;
import com.extractor.unraveldocs.auth.dto.request.LoginRequestDto;
import com.extractor.unraveldocs.auth.interfaces.LoginUserService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.TokenProcessingException;
import com.extractor.unraveldocs.exceptions.custom.UnauthorizedException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.loginattempts.interfaces.LoginAttemptsService;
import com.extractor.unraveldocs.security.JwtTokenProvider;
import com.extractor.unraveldocs.security.RefreshTokenService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUserImpl implements LoginUserService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptsService loginAttemptsService;
    private final ResponseBuilderService responseBuilder;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public LoginResult loginUser(LoginRequestDto request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        // Block login for soft-deleted accounts (Issue #2)
        userOpt.ifPresent(user -> {
            if (user.getDeletedAt() != null) {
                throw new BadRequestException(
                        "This account has been deactivated. Please contact support.",
                        "ACCOUNT_DEACTIVATED");
            }
        });

        userOpt.ifPresent(loginAttemptsService::checkIfUserBlocked);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            userOpt.ifPresent(loginAttemptsService::recordFailedLoginAttempt);
            throw new UnauthorizedException("Invalid email or password", "INVALID_CREDENTIALS");
        } catch (DisabledException e) {
            throw new ForbiddenException(
                    "User account is disabled. Please verify your email or contact support.",
                    "ACCOUNT_NOT_VERIFIED");
        } catch (LockedException e) {
            throw new ForbiddenException(
                    "User account is locked. Please contact support or try again later.",
                    "ACCOUNT_LOCKED");
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user {}: {}", request.email(), e.getMessage());
            userOpt.ifPresent(loginAttemptsService::recordFailedLoginAttempt);
            throw new UnauthorizedException(
                    "Authentication failed. Please check your credentials.",
                    "INVALID_CREDENTIALS");
        }

        User authenticatedUser = (User) authentication.getPrincipal();

        loginAttemptsService.resetLoginAttempts(authenticatedUser);

        assert authenticatedUser != null;
        String accessToken = jwtTokenProvider.generateAccessToken(authenticatedUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authenticatedUser);

        String refreshTokenJti = jwtTokenProvider.getJtiFromToken(refreshToken);
        if (refreshTokenJti != null) {
            refreshTokenService.storeRefreshToken(refreshTokenJti, authenticatedUser.getId());
        } else {
            log.error("Could not generate JTI for refresh token for user {}", authenticatedUser.getEmail());
            throw new TokenProcessingException("Error processing refresh token.");
        }

        authenticatedUser.setLastLogin(OffsetDateTime.now());
        userRepository.save(authenticatedUser);

        // Issue #8: Login response contains only token data — profile via GET
        // /api/v1/user/me
        LoginData data = LoginData.builder()
                .userId(authenticatedUser.getId())
                .accessToken(accessToken)
                .tokenType("Bearer")
                .accessExpiresIn(jwtTokenProvider.getAccessExpirationInMs())
                .build();

        UnravelDocsResponse<LoginData> response = responseBuilder.buildUserResponse(
                data, HttpStatus.OK, "User logged in successfully");

        // Return refresh token separately for cookie setting by controller
        return new LoginResult(response, refreshToken);
    }
}
