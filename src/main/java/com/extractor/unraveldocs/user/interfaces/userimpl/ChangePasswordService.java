package com.extractor.unraveldocs.user.interfaces.userimpl;

import com.extractor.unraveldocs.user.dto.request.ChangePasswordDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface ChangePasswordService {
    UnravelDocsResponse<Void> changePassword(ChangePasswordDto request);
}
