package com.extractor.unraveldocs.user.interfaces.userimpl;

import com.extractor.unraveldocs.user.dto.request.ForgotPasswordDto;
import com.extractor.unraveldocs.user.dto.request.ResetPasswordDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.interfaces.passwordreset.IPasswordReset;

public interface PasswordResetService {
    UnravelDocsDataResponse<Void> resetPassword(IPasswordReset params, ResetPasswordDto request);
    UnravelDocsDataResponse<Void> forgotPassword(ForgotPasswordDto forgotPasswordDto);
}
