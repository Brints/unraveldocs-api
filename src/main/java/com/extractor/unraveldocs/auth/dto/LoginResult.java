package com.extractor.unraveldocs.auth.dto;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

/**
 * Wrapper returned by the login service layer. Holds the JSON response body
 * (tokens-only LoginData) and the raw refresh token for cookie setting
 * by the controller.
 */
public record LoginResult(
        UnravelDocsResponse<LoginData> response,
        String refreshToken) {
}
