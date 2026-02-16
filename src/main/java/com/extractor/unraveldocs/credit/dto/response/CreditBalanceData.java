package com.extractor.unraveldocs.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user credit balance.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditBalanceData {
    private Integer balance;
    private Integer totalPurchased;
    private Integer totalUsed;
}
