package com.extractor.unraveldocs.metrics.dashboard.service.impl;

import com.extractor.unraveldocs.metrics.dashboard.dto.DashboardKpiStatsDto;
import com.extractor.unraveldocs.metrics.dashboard.service.DashboardMetricsService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardMetricsServiceImpl implements DashboardMetricsService {

    private final DashboardMetricsCacheService dashboardMetricsCacheService;
    private final ResponseBuilderService responseBuilderService;

    @Override
    public UnravelDocsResponse<DashboardKpiStatsDto> getDashboardKpiStats() {
        DashboardKpiStatsDto statsDto = dashboardMetricsCacheService.getDashboardKpiStatsData();
        log.info("Returning Dashboard KPI Stats response");

        return responseBuilderService.buildUserResponse(statsDto, HttpStatus.OK, "Successfully fetched dashboard KPI stats");
    }
}
