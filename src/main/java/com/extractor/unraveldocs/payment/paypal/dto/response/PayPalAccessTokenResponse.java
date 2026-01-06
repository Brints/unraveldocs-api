package com.extractor.unraveldocs.payment.paypal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for PayPal OAuth2 access token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalAccessTokenResponse {

    private String scope;
    private String accessToken;
    private String tokenType;
    private String appId;
    private Integer expiresIn;
    private String nonce;

    /**
     * Check if the token is valid.
     */
    public boolean isValid() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Get the authorization header value.
     */
    public String getAuthorizationHeader() {
        return tokenType + " " + accessToken;
    }
}
