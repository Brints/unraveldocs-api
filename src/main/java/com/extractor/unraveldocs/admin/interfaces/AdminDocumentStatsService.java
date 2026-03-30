package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.DocumentStatsDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminDocumentStatsService {
    UnravelDocsResponse<DocumentStatsDto> getDocumentStats();
}
