package com.extractor.unraveldocs.subscription.jobs;

import com.extractor.unraveldocs.subscription.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to refresh exchange rates daily.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateUpdateJob {

    private final CurrencyConversionService currencyConversionService;

    /**
     * Refresh exchange rates daily at 6:00 AM.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void refreshExchangeRates() {
        log.info("Starting scheduled exchange rate refresh...");
        try {
            currencyConversionService.refreshRates();
            log.info("Completed scheduled exchange rate refresh");
        } catch (Exception e) {
            log.error("Failed to refresh exchange rates: {}", e.getMessage(), e);
        }
    }
}
