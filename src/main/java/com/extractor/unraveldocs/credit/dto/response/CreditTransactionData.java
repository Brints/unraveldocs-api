package com.extractor.unraveldocs.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for credit transaction history.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditTransactionData {
    private String transactionId;
    private String type;
    private Integer amount;
    private Integer balanceAfter;
    private String description;
    private OffsetDateTime createdAt;
}
