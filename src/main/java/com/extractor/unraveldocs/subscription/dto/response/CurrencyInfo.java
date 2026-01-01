package com.extractor.unraveldocs.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a supported currency for dropdown selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyInfo {

    /**
     * Currency code (e.g., "USD", "NGN", "EUR").
     */
    private String code;

    /**
     * Currency symbol (e.g., "$", "₦", "€").
     */
    private String symbol;

    /**
     * Full currency name (e.g., "United States Dollar", "Nigerian Naira").
     */
    private String name;
}
