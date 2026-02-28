package com.extractor.unraveldocs.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for credit pack purchase initialization.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditPurchaseData {
    private String paymentUrl;
    private String reference;
    private String packName;
    private Integer creditsToReceive;
    private Long amountInCents;
    private Long discountApplied;

    // Currency info
    private String currency;
    private String formattedAmount;
    private BigDecimal exchangeRate;
}
