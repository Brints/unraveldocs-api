package com.extractor.unraveldocs.auth.impl;

import com.extractor.unraveldocs.auth.dto.RefreshLoginData;
import com.extractor.unraveldocs.auth.dto.RefreshResult;
import com.extractor.unraveldocs.auth.interfaces.RefreshTokenService;
import com.extractor.unraveldocs.auth.service.CustomUserDetailsService;
import com.extractor.unraveldocs.exceptions.custom.UnauthorizedException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.security.JwtTokenProvider;
import com.extractor.unraveldocs.security.TokenBlacklistService;
import com.extractor.unraveldocs.user.model.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenImpl implements RefreshTokenService {
    private final JwtTokenProvider jwtTokenProvider;
    private final com.extractor.unraveldocs.security.RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomUserDetailsService customUserDetailsService;
    private final ResponseBuilderService responseBuilder;

    @Override
    public RefreshResult refreshToken(String requestRefreshToken) {
        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required.", "TOKEN_MISSING");
        }

        String refreshTokenJti = jwtTokenProvider.getJtiFromToken(requestRefreshToken);

        if (refreshTokenJti == null ||
                !jwtTokenProvider.validateToken(requestRefreshToken) ||
                !refreshTokenService.validateRefreshToken(refreshTokenJti)) {
            throw new UnauthorizedException("Invalid or expired refresh token.", "TOKEN_INVALID");
        }

        Claims claims = jwtTokenProvider.getAllClaimsFromToken(requestRefreshToken);
        String tokenType = claims.get("type", String.class);
        if (!"REFRESH".equals(tokenType)) {
            throw new UnauthorizedException("Invalid token type for refresh.", "TOKEN_INVALID");
        }

        String userId = refreshTokenService.getUserIdByTokenJti(refreshTokenJti);
        User user = customUserDetailsService.loadUserEntityById(userId);

        if (!user.isVerified()) {
            throw new UnauthorizedException("User account is not active or verified.", "ACCOUNT_NOT_VERIFIED");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        // Rolling refresh tokens — invalidate old, issue new
        refreshTokenService.deleteRefreshToken(refreshTokenJti);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        String newRefreshTokenJti = jwtTokenProvider.getJtiFromToken(newRefreshToken);
        refreshTokenService.storeRefreshToken(newRefreshTokenJti, String.valueOf(user.getId()));

        // Issue #4/#8: Only access token data in JSON body; refresh token set as cookie
        // by controller
        RefreshLoginData loginData = new RefreshLoginData(
                newAccessToken,
                "Bearer",
                jwtTokenProvider.getAccessExpirationInMs());

        UnravelDocsResponse<RefreshLoginData> response = responseBuilder.buildUserResponse(
                loginData,
                HttpStatus.OK,
                "Token refreshed successfully");

        return new RefreshResult(response, newRefreshToken);
    }

    @Override
    public UnravelDocsResponse<Void> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String accessToken = bearerToken.substring(7);
            if (jwtTokenProvider.validateToken(accessToken)) {
                String jti = jwtTokenProvider.getJtiFromToken(accessToken);
                if (jti != null) {
                    Claims claims = jwtTokenProvider.getAllClaimsFromToken(accessToken);
                    Date expirationDate = claims.getExpiration();
                    long expiresInSeconds = 0;
                    if (expirationDate != null) {
                        expiresInSeconds = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
                    }

                    if (expiresInSeconds > 0) {
                        tokenBlacklistService.blacklistToken(jti, expiresInSeconds);
                    }

                    // Issue #1: Invalidate all refresh tokens for this user
                    String userId = claims.get("userId", String.class);
                    if (userId != null) {
                        refreshTokenService.deleteAllRefreshTokensForUser(userId);
                    }
                }
            }
        }
        SecurityContextHolder.clearContext();
        return responseBuilder.buildUserResponse(
                null,
                HttpStatus.NO_CONTENT,
                "Logged out successfully");
    }

    @Override
    public UnravelDocsResponse<Void> logoutAllDevices(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String accessToken = bearerToken.substring(7);
            if (jwtTokenProvider.validateToken(accessToken)) {
                String jti = jwtTokenProvider.getJtiFromToken(accessToken);
                if (jti != null) {
                    Claims claims = jwtTokenProvider.getAllClaimsFromToken(accessToken);
                    Date expirationDate = claims.getExpiration();
                    long expiresInSeconds = 0;
                    if (expirationDate != null) {
                        expiresInSeconds = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
                    }

                    if (expiresInSeconds > 0) {
                        tokenBlacklistService.blacklistToken(jti, expiresInSeconds);
                    }

                    String userId = claims.get("userId", String.class);
                    if (userId != null) {
                        refreshTokenService.deleteAllRefreshTokensForUser(userId);
                    }
                }
            }
        }
        SecurityContextHolder.clearContext();
        return responseBuilder.buildUserResponse(
                null,
                HttpStatus.NO_CONTENT,
                "Logged out from all devices successfully");
    }
}
