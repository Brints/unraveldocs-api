package com.extractor.unraveldocs.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for credit pack details.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditPackData {
    private String id;
    private String name;
    private String displayName;
    private Long priceInCents;
    private String currency;
    private Integer creditsIncluded;
    private BigDecimal costPerCredit;

    // Optional currency conversion fields (populated when ?currency= is provided)
    private Long convertedPriceInCents;
    private String convertedCurrency;
    private String formattedPrice;
    private String formattedOriginalPrice;
    private BigDecimal exchangeRate;
}
