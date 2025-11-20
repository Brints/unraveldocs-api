package com.extractor.unraveldocs.user.interfaces.userimpl;

import com.extractor.unraveldocs.user.dto.request.ForgotPasswordDto;
import com.extractor.unraveldocs.user.dto.request.ResetPasswordDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.interfaces.passwordreset.IPasswordReset;

public interface PasswordResetService {
    UnravelDocsResponse<Void> resetPassword(IPasswordReset params, ResetPasswordDto request);
    UnravelDocsResponse<Void> forgotPassword(ForgotPasswordDto forgotPasswordDto);
}
