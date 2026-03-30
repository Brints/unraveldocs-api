package com.extractor.unraveldocs.metrics.dashboard.service;

import com.extractor.unraveldocs.metrics.dashboard.dto.DashboardKpiStatsDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface DashboardMetricsService {
    UnravelDocsResponse<DashboardKpiStatsDto> getDashboardKpiStats();
}
