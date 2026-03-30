package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.CreditStatsDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminCreditStatsService {
    UnravelDocsResponse<CreditStatsDto> getCreditStats();
}
