package com.extractor.unraveldocs.subscription.controller;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.dto.response.AllPlansWithPricingResponse;
import com.extractor.unraveldocs.subscription.dto.response.SupportedCurrenciesResponse;
import com.extractor.unraveldocs.subscription.service.PlanPricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST controller for retrieving subscription plan pricing with currency
 * conversion.
 * These endpoints are publicly accessible for marketing/pricing pages.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plan Pricing", description = "Public APIs for retrieving subscription plan pricing with currency conversion")
public class PlanPricingController {

    private final PlanPricingService planPricingService;
    private final SanitizeLogging sanitizer;

    @Operation(summary = "Get all subscription plans with pricing", description = "Retrieves all individual and team subscription plans with prices converted to the specified currency."
            +
            "This endpoint is public and can be used on marketing/pricing pages.", responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved plans with pricing", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AllPlansWithPricingResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid currency code provided")
            })
    @GetMapping
    public ResponseEntity<AllPlansWithPricingResponse> getAllPlansWithPricing(
            @Parameter(description = "Currency code for price conversion (e.g., USD, NGN, EUR, GBP). " +
                    "Defaults to USD if not specified.", example = "USD") @RequestParam(value = "currency", required = false, defaultValue = "USD") String currencyCode) {

        log.info("Request received for plans pricing in currency: {}", sanitizer.sanitizeLogging(currencyCode));

        SubscriptionCurrency currency;
        try {
            currency = SubscriptionCurrency.fromIdentifier(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid currency code provided: {}", sanitizer.sanitizeLogging(currencyCode));
            throw new IllegalArgumentException(
                    "Invalid currency code: " + currencyCode
                            + ". Use GET /api/v1/plans/currencies to see supported currencies.");
        }

        AllPlansWithPricingResponse response = planPricingService.getAllPlansWithPricing(currency);

        log.info("Returning {} individual plans and {} team plans in {}",
                sanitizer.sanitizeLoggingInteger(response.getIndividualPlans().size()),
                sanitizer.sanitizeLoggingInteger(response.getTeamPlans().size()),
                sanitizer.sanitizeLogging(currency.getCode()));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get list of supported currencies", description = "Retrieves all supported currencies for the pricing dropdown. "
            +
            "This endpoint is public and can be used to populate currency selection dropdowns.", responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved supported currencies", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SupportedCurrenciesResponse.class)))
            })
    @GetMapping("/currencies")
    public ResponseEntity<SupportedCurrenciesResponse> getSupportedCurrencies() {
        log.info("Request received for supported currencies list");

        SupportedCurrenciesResponse response = planPricingService.getSupportedCurrencies();

        log.info("Returning {} supported currencies", sanitizer.sanitizeLoggingInteger(response.getTotalCount()));

        return ResponseEntity.ok(response);
    }
}
