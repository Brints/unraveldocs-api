package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.RefreshResult;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenService {
    RefreshResult refreshToken(String refreshToken);

    UnravelDocsResponse<Void> logout(HttpServletRequest request);

    UnravelDocsResponse<Void> logoutAllDevices(HttpServletRequest request);
}
