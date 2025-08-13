package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import org.springframework.security.core.Authentication;

public interface ChangeUserRoleService {
    UnravelDocsDataResponse<AdminData> changeUserRole(ChangeRoleDto request, Authentication authentication);
}
