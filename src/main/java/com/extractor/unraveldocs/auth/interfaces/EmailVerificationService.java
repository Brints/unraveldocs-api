package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.request.ResendEmailVerificationDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface EmailVerificationService {
    UnravelDocsResponse<Void> verifyEmail(String email, String token);
    UnravelDocsResponse<Void> resendEmailVerification(ResendEmailVerificationDto request);
}
