package com.extractor.unraveldocs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final Long refreshTokenDurationMs;
    private static final String REFRESH_TOKEN_PREFIX = "refreshtoken:";

    public RefreshTokenService(StringRedisTemplate redisTemplate,
            @Value("${app.jwt-refresh-token-expiration-milliseconds}") Long refreshTokenDurationMs) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenDurationMs = refreshTokenDurationMs;
    }

    public void storeRefreshToken(String tokenJti, String userId) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + tokenJti, userId, refreshTokenDurationMs,
                TimeUnit.MILLISECONDS);
    }

    public String getUserIdByTokenJti(String tokenJti) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + tokenJti);
    }

    public boolean validateRefreshToken(String tokenJti) {
        return redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + tokenJti);
    }

    public void deleteRefreshToken(String tokenJti) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + tokenJti);
    }

    /**
     * Deletes all refresh tokens for a given user by scanning Redis keys.
     * Used for logout (invalidate current user's sessions) and logout-all-devices.
     */
    public void deleteAllRefreshTokensForUser(String userId) {
        Set<String> keys = redisTemplate.keys(REFRESH_TOKEN_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                String storedUserId = redisTemplate.opsForValue().get(key);
                if (userId.equals(storedUserId)) {
                    redisTemplate.delete(key);
                }
            }
        }
    }
}