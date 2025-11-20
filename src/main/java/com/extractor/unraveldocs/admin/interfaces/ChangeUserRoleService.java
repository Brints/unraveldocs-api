package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import org.springframework.security.core.Authentication;

public interface ChangeUserRoleService {
    UnravelDocsResponse<AdminData> changeUserRole(ChangeRoleDto request, Authentication authentication);
}
