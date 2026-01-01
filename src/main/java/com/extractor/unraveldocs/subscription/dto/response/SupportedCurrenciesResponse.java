package com.extractor.unraveldocs.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing the list of supported currencies for frontend
 * dropdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportedCurrenciesResponse {

    /**
     * List of all supported currencies.
     */
    private List<CurrencyInfo> currencies;

    /**
     * Total number of supported currencies.
     */
    private int totalCount;
}
