package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.ActionReasonDto;
import com.extractor.unraveldocs.admin.interfaces.AdminUserActionService;
import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.security.JwtTokenProvider;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.dto.request.ForgotPasswordDto;
import com.extractor.unraveldocs.user.interfaces.userimpl.PasswordResetService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserActionServiceImpl implements AdminUserActionService {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public UnravelDocsResponse<String> toggleUserStatus(String userId, boolean isActive, ActionReasonDto request) {
        User user = getUser(userId);
        user.setActive(isActive);
        userRepository.save(user);

        log.info("Admin toggled user {} active status to {}. Reason: {}", userId, isActive, request.getReason());
        String action = isActive ? "activated" : "deactivated";
        return new UnravelDocsResponse<>(200, "success", "User successfully " + action, null);
    }

    @Override
    @Transactional
    public UnravelDocsResponse<String> forceVerifyEmail(String userId) {
        User user = getUser(userId);
        if (user.isVerified()) {
            return new UnravelDocsResponse<>(200, "success", "User is already verified.", null);
        }
        user.setVerified(true);
        userRepository.save(user);

        log.info("Admin forcefully verified user {}", userId);
        return new UnravelDocsResponse<>(200, "success", "User email successfully verified", null);
    }

    @Override
    @Transactional
    public UnravelDocsResponse<String> unlockUser(String userId) {
        User user = getUser(userId);
        if (user.getLoginAttempts() != null) {
            user.getLoginAttempts().setLoginAttempts(0);
            user.getLoginAttempts().setBlockedUntil(null);
        }
        userRepository.save(user);

        log.info("Admin unlocked user {}", userId);
        return new UnravelDocsResponse<>(200, "success", "User account unlocked", null);
    }

    @Override
    public UnravelDocsResponse<String> triggerPasswordReset(String userId) {
        User user = getUser(userId);
        ForgotPasswordDto dto = ForgotPasswordDto.builder().email(user.getEmail()).build();
        passwordResetService.forgotPassword(dto);

        log.info("Admin triggered password reset for user {}", userId);
        return new UnravelDocsResponse<>(200, "success", "Password reset email triggered successfully", null);
    }

    @Override
    @Transactional
    public UnravelDocsResponse<String> softDeleteUser(String userId, ActionReasonDto request) {
        User user = getUser(userId);
        user.setDeletedAt(OffsetDateTime.now());
        user.setActive(false);
        userRepository.save(user);

        log.info("Admin soft-deleted user {}. Reason: {}", userId, request.getReason());
        return new UnravelDocsResponse<>(200, "success", "User successfully soft-deleted", null);
    }

    @Override
    public UnravelDocsResponse<LoginData> impersonateUser(String userId, ActionReasonDto request) {
        User user = getUser(userId);
        
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        LoginData loginData = LoginData.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .tokenType("Bearer")
                .accessExpiresIn(jwtTokenProvider.getAccessExpirationInMs())
                .build();

        log.warn("ADMIN IMPERSONATION: Support impersonated user {}. Reason: {}", userId, request.getReason());
        return new UnravelDocsResponse<>(200, "success", "Impersonation token generated successfully", loginData);
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
