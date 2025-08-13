package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.RefreshLoginData;
import com.extractor.unraveldocs.auth.dto.request.RefreshTokenRequest;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenService {
    UnravelDocsDataResponse<RefreshLoginData> refreshToken(RefreshTokenRequest refreshTokenRequest);
    UnravelDocsDataResponse<Void> logout(HttpServletRequest request);
}
