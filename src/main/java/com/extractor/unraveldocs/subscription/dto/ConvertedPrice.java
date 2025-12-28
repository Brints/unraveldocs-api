package com.extractor.unraveldocs.subscription.dto;

import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO representing a price converted from USD to a target currency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertedPrice {

    /**
     * Original amount in USD.
     */
    private BigDecimal originalAmountUsd;

    /**
     * Converted amount in target currency.
     */
    private BigDecimal convertedAmount;

    /**
     * Target currency.
     */
    private SubscriptionCurrency currency;

    /**
     * Formatted price string (e.g., "₦45,000.00").
     */
    private String formattedPrice;

    /**
     * Exchange rate used for conversion (1 USD = X target currency).
     */
    private BigDecimal exchangeRate;

    /**
     * Timestamp when the exchange rate was last updated.
     */
    private OffsetDateTime rateTimestamp;
}
