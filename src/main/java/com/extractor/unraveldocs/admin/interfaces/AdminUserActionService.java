package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.request.ActionReasonDto;
import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminUserActionService {
    UnravelDocsResponse<String> toggleUserStatus(String userId, boolean isActive, ActionReasonDto request);
    UnravelDocsResponse<String> forceVerifyEmail(String userId);
    UnravelDocsResponse<String> unlockUser(String userId);
    UnravelDocsResponse<String> triggerPasswordReset(String userId);
    UnravelDocsResponse<String> softDeleteUser(String userId, ActionReasonDto request);
    UnravelDocsResponse<LoginData> impersonateUser(String userId, ActionReasonDto request);
}
