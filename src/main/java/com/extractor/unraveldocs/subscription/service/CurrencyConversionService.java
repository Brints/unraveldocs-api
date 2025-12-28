package com.extractor.unraveldocs.subscription.service;

import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;

import java.math.BigDecimal;

/**
 * Service for converting prices between currencies.
 * Prices are stored in USD and converted to user's local currency.
 */
public interface CurrencyConversionService {

    /**
     * Convert an amount from USD to the target currency.
     *
     * @param amountUsd      Amount in USD
     * @param targetCurrency Target currency to convert to
     * @return ConvertedPrice with original and converted amounts
     */
    ConvertedPrice convert(BigDecimal amountUsd, SubscriptionCurrency targetCurrency);

    /**
     * Get the current exchange rate for a currency (1 USD = X currency).
     *
     * @param currency Target currency
     * @return Exchange rate
     */
    BigDecimal getExchangeRate(SubscriptionCurrency currency);

    /**
     * Refresh exchange rates from external API.
     * Called by scheduled job.
     */
    void refreshRates();

    /**
     * Format a price with currency symbol and proper formatting.
     *
     * @param amount   Amount to format
     * @param currency Currency for formatting
     * @return Formatted price string (e.g., "₦45,000.00")
     */
    String formatPrice(BigDecimal amount, SubscriptionCurrency currency);
}
