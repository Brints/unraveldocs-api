package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.SecurityStatsDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminSecurityStatsService {
    UnravelDocsResponse<SecurityStatsDto> getSecurityStats();
}
