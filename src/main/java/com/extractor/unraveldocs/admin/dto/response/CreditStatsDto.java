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
public class CreditStatsDto {
    private long totalCreditsInCirculation;
    private long totalCreditsPurchased;
    private long totalCreditsUsed;
    private long totalCreditTransactions;
    private Map<String, Long> transactionsByType;
    private long activeCreditPacks;
    private long usersWithZeroBalance;
    private BigDecimal averageCreditBalance;
}
