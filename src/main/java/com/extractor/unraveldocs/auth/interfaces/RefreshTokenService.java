package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.RefreshLoginData;
import com.extractor.unraveldocs.auth.dto.request.RefreshTokenRequest;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenService {
    UnravelDocsResponse<RefreshLoginData> refreshToken(RefreshTokenRequest refreshTokenRequest);
    UnravelDocsResponse<Void> logout(HttpServletRequest request);
}
