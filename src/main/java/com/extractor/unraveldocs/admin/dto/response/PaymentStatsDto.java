package com.extractor.unraveldocs.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO containing aggregated payment and revenue statistics for the admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsDto {
    private long totalTransactions;
    private BigDecimal totalRevenue;
    private Map<String, BigDecimal> revenueByProvider;
    private Map<String, BigDecimal> revenueByCurrency;
    private Map<String, BigDecimal> revenueByPaymentMethod;
    private BigDecimal averageTransactionValue;
    private long pendingReceiptEmails;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;
}
