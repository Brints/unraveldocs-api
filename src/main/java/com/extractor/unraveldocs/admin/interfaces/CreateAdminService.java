package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.CreateAdminRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface CreateAdminService {
    UnravelDocsResponse<AdminData> createAdminUser(CreateAdminRequestDto request);
}
