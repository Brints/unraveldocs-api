package com.extractor.unraveldocs.auth.dto;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

/**
 * Wrapper returned by the refresh-token service layer. Holds the JSON response
 * body (tokens-only RefreshLoginData) and the raw new refresh token for cookie
 * setting by the controller.
 */
public record RefreshResult(
        UnravelDocsResponse<RefreshLoginData> response,
        String refreshToken) {
}
