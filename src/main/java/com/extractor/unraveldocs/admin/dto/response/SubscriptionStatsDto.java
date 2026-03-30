package com.extractor.unraveldocs.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatsDto {
    private long totalSubscriptions;
    private Map<String, Long> byPlan;
    private Map<String, Long> byStatus;
    private Map<String, Long> bySource;
    private double trialConversionRate;
    private double churnRate;
    private BigDecimal mrr;
    private BigDecimal averageRevenuePerUser;
    private QuotaWarningStats usersNearingQuotaLimits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotaWarningStats {
        private long storage;
        private long ocrPages;
        private long aiOperations;
        private long documentUploads;
    }
}
