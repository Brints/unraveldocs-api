package com.extractor.unraveldocs.subscription.controller;

import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;
import com.extractor.unraveldocs.subscription.service.CurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public controller for currency conversion operations.
 * Provides general-purpose currency conversion and exchange rate endpoints.
 */
@RestController
@RequestMapping("/api/v1/currency")
@RequiredArgsConstructor
@Tag(name = "Currency", description = "Currency conversion and exchange rates")
public class CurrencyController {

    private final CurrencyConversionService currencyConversionService;
    private final ResponseBuilderService responseBuilderService;

    @Operation(summary = "Convert an amount between currencies", description = "Converts an amount from one currency to another using cached exchange rates. "
            +
            "Amount should be in the smallest unit (e.g., cents for USD, kobo for NGN).")
    @GetMapping("/convert")
    public ResponseEntity<UnravelDocsResponse<ConvertedPrice>> convert(
            @RequestParam long amountInCents,
            @RequestParam(defaultValue = "USD") String from,
            @RequestParam String to) {

        SubscriptionCurrency fromCurrency = SubscriptionCurrency.fromIdentifier(from);
        SubscriptionCurrency toCurrency = SubscriptionCurrency.fromIdentifier(to);

        // Convert cents to main unit (e.g., 500 cents → $5.00)
        BigDecimal amountInMainUnit = BigDecimal.valueOf(amountInCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // If from-currency is not USD, first convert to USD
        BigDecimal amountInUsd;
        if (fromCurrency == SubscriptionCurrency.USD) {
            amountInUsd = amountInMainUnit;
        } else {
            BigDecimal fromRate = currencyConversionService.getExchangeRate(fromCurrency);
            amountInUsd = amountInMainUnit.divide(fromRate, 6, RoundingMode.HALF_UP);
        }

        // Convert from USD to target currency
        ConvertedPrice result = currencyConversionService.convert(amountInUsd, toCurrency);

        return ResponseEntity.ok(
                responseBuilderService.buildUserResponse(result, HttpStatus.OK, "Currency converted"));
    }

    @Operation(summary = "Get exchange rates", description = "Returns exchange rates for all supported currencies relative to USD")
    @GetMapping("/rates")
    public ResponseEntity<UnravelDocsResponse<Map<String, Object>>> getRates() {
        Map<String, Object> ratesData = new LinkedHashMap<>();
        ratesData.put("baseCurrency", "USD");

        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        for (SubscriptionCurrency currency : SubscriptionCurrency.values()) {
            rates.put(currency.getCode(), currencyConversionService.getExchangeRate(currency));
        }
        ratesData.put("rates", rates);

        return ResponseEntity.ok(
                responseBuilderService.buildUserResponse(ratesData, HttpStatus.OK, "Exchange rates retrieved"));
    }

    @Operation(summary = "List supported currencies", description = "Returns all supported currencies with their codes, symbols, and names")
    @GetMapping("/supported")
    public ResponseEntity<UnravelDocsResponse<Map<String, Object>[]>> getSupportedCurrencies() {
        @SuppressWarnings("unchecked")
        Map<String, Object>[] currencies = new Map[SubscriptionCurrency.values().length];
        int i = 0;
        for (SubscriptionCurrency currency : SubscriptionCurrency.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", currency.getCode());
            entry.put("symbol", currency.getSymbol());
            entry.put("name", currency.getFullName());
            currencies[i++] = entry;
        }

        return ResponseEntity.ok(
                responseBuilderService.buildUserResponse(currencies, HttpStatus.OK, "Supported currencies retrieved"));
    }
}
