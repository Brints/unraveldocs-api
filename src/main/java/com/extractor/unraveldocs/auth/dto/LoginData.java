package com.extractor.unraveldocs.auth.dto;

import lombok.Builder;

/**
 * Token-only login response DTO. Profile data is served separately
 * via GET /api/v1/user/me.
 */
@Builder
public record LoginData(
        String userId,
        String accessToken,
        String tokenType,
        Long accessExpiresIn) {
}
