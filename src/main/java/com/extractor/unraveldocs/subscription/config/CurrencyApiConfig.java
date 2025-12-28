package com.extractor.unraveldocs.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the exchange rate API.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "currency.api")
public class CurrencyApiConfig {

    /**
     * API key for exchangerate-api.com
     */
    private String key;

    /**
     * Base URL for the API (default: exchangerate-api.com)
     */
    private String baseUrl = "https://v6.exchangerate-api.com/v6";

    /**
     * Whether to enable caching of exchange rates.
     */
    private boolean cachingEnabled = true;

    /**
     * Cache TTL in hours (default: 24 hours).
     */
    private int cacheTtlHours = 24;

    /**
     * Fallback rates to use when API is unavailable.
     * These are approximate rates and should be updated periodically.
     */
    private boolean useFallbackRates = true;
}
