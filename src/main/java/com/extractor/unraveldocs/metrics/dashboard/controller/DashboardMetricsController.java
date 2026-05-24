package com.extractor.unraveldocs.metrics.dashboard.controller;

import com.extractor.unraveldocs.metrics.dashboard.dto.DashboardKpiStatsDto;
import com.extractor.unraveldocs.metrics.dashboard.service.DashboardMetricsService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard Metrics", description = "Admin endpoints for dashboard statistics and KPIs")
public class DashboardMetricsController {
    
    private final DashboardMetricsService dashboardMetricsService;

    @Operation(summary = "Get Dashboard KPI Stats", description = "Fetches aggregate KPIs for users, subscriptions, and storage")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR') or hasRole('SUPER_ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<UnravelDocsResponse<DashboardKpiStatsDto>> getDashboardKpiStats() {
        return ResponseEntity.ok(dashboardMetricsService.getDashboardKpiStats());
    }
}
