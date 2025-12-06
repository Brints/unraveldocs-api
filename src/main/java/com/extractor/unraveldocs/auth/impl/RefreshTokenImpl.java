package com.extractor.unraveldocs.auth.impl;

import com.extractor.unraveldocs.auth.dto.RefreshLoginData;
import com.extractor.unraveldocs.auth.dto.request.RefreshTokenRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class RefreshTokenImpl implements RefreshTokenService {
    private final JwtTokenProvider jwtTokenProvider;
    private final com.extractor.unraveldocs.security.RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomUserDetailsService customUserDetailsService;
    private final ResponseBuilderService responseBuilder;

    @Override
    public UnravelDocsResponse<RefreshLoginData> refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        String refreshTokenJti = jwtTokenProvider.getJtiFromToken(requestRefreshToken);

        if (
                refreshTokenJti == null ||
                !jwtTokenProvider.validateToken(requestRefreshToken) ||
                !refreshTokenService.validateRefreshToken(refreshTokenJti)) {
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }

        Claims claims = jwtTokenProvider.getAllClaimsFromToken(requestRefreshToken);
        String tokenType = claims.get("type", String.class);
        if (!"REFRESH".equals(tokenType)) {
            throw new UnauthorizedException("Invalid token type for refresh.");
        }

        String userId = refreshTokenService.getUserIdByTokenJti(refreshTokenJti);
        User user = customUserDetailsService.loadUserEntityById(userId);

        if (!user.isVerified()) {
            throw new UnauthorizedException("User account is not active or verified.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        // Implement rolling refresh tokens (invalidate old, issue new)
         refreshTokenService.deleteRefreshToken(refreshTokenJti);
         String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
         String newRefreshTokenJti = jwtTokenProvider.getJtiFromToken(newRefreshToken);
         refreshTokenService.storeRefreshToken(newRefreshTokenJti, String.valueOf(user.getId()));

        RefreshLoginData loginData = new RefreshLoginData(
                user.getId(),
                user.getEmail(),
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenProvider.getAccessExpirationInMs()

        );

        return responseBuilder.buildUserResponse(
                loginData,
                HttpStatus.OK,
                "Token refreshed successfully"
        );
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
                    // TODO: Handle the case where the token is already blacklisted or invalid.
                    // Optionally, invalidate the refresh token associated with this session/user.
                    // This requires more complex logic, e.g., storing refresh token JTI per user session
                    // or finding all refresh tokens for a user and deleting them.
                    // For simplicity, we are only blacklisting the access token here.
                }
            }
        }
        SecurityContextHolder.clearContext(); // Clear security context
        return responseBuilder.buildUserResponse(
                null,
                HttpStatus.OK,
                "Logged out successfully"
        );
    }
}
