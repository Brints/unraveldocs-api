package com.extractor.unraveldocs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token-only refresh response DTO. The new refresh token is set as an
 * HttpOnly cookie and not returned in the JSON body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshLoginData {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long accessExpiresIn;
}