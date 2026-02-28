package com.extractor.unraveldocs.credit.service;

import com.extractor.unraveldocs.credit.datamodel.CreditPackName;
import com.extractor.unraveldocs.credit.dto.request.CreateCreditPackRequest;
import com.extractor.unraveldocs.credit.dto.request.UpdateCreditPackRequest;
import com.extractor.unraveldocs.credit.dto.response.CreditPackData;
import com.extractor.unraveldocs.credit.model.CreditPack;
import com.extractor.unraveldocs.credit.repository.CreditPackRepository;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;
import com.extractor.unraveldocs.subscription.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service for admin management of credit packs.
 * Provides CRUD operations for credit pack configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditPackManagementService {

    private final CreditPackRepository creditPackRepository;
    private final CurrencyConversionService currencyConversionService;

    /**
     * Get all active credit packs (for user-facing listing).
     */
    public List<CreditPackData> getActivePacks() {
        return creditPackRepository.findByIsActiveTrue().stream()
                .map(this::toPackData)
                .toList();
    }

    /**
     * Get all active credit packs with prices converted to the specified currency.
     *
     * @param targetCurrencyCode The currency code (e.g., "NGN", "EUR")
     * @return Packs with both USD and converted prices
     */
    public List<CreditPackData> getActivePacksWithCurrency(String targetCurrencyCode) {
        SubscriptionCurrency targetCurrency = SubscriptionCurrency.fromIdentifier(targetCurrencyCode);

        return creditPackRepository.findByIsActiveTrue().stream()
                .map(pack -> toPackDataWithCurrency(pack, targetCurrency))
                .toList();
    }

    /**
     * Get all credit packs including inactive (for admin view).
     */
    public List<CreditPackData> getAllPacks() {
        return creditPackRepository.findAll().stream()
                .map(this::toPackData)
                .toList();
    }

    /**
     * Get a single credit pack by ID.
     */
    public CreditPackData getPackById(String packId) {
        CreditPack pack = findPackOrThrow(packId);
        return toPackData(pack);
    }

    /**
     * Create a new credit pack (Admin).
     */
    @Transactional
    public CreditPackData createPack(CreateCreditPackRequest request) {
        CreditPackName packName;
        try {
            packName = CreditPackName.valueOf(request.getName().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid credit pack name: " + request.getName()
                    + ". Valid names: STARTER_PACK, VALUE_PACK, POWER_PACK");
        }

        if (creditPackRepository.findByName(packName).isPresent()) {
            throw new BadRequestException("Credit pack with name '" + packName + "' already exists");
        }

        CreditPack pack = CreditPack.builder()
                .name(packName)
                .displayName(request.getDisplayName())
                .priceInCents(request.getPriceInCents())
                .currency(request.getCurrency())
                .creditsIncluded(request.getCreditsIncluded())
                .isActive(true)
                .build();

        CreditPack saved = creditPackRepository.save(pack);
        return toPackData(saved);
    }

    /**
     * Update an existing credit pack (Admin).
     * Only provided (non-null) fields are updated.
     */
    @Transactional
    public CreditPackData updatePack(String packId, UpdateCreditPackRequest request) {
        CreditPack pack = findPackOrThrow(packId);

        if (request.getDisplayName() != null) {
            pack.setDisplayName(request.getDisplayName());
        }
        if (request.getPriceInCents() != null) {
            pack.setPriceInCents(request.getPriceInCents());
        }
        if (request.getCreditsIncluded() != null) {
            pack.setCreditsIncluded(request.getCreditsIncluded());
        }
        if (request.getIsActive() != null) {
            pack.setIsActive(request.getIsActive());
        }

        CreditPack updated = creditPackRepository.save(pack);
        return toPackData(updated);
    }

    /**
     * Deactivate a credit pack (soft delete).
     */
    @Transactional
    public void deactivatePack(String packId) {
        CreditPack pack = findPackOrThrow(packId);
        pack.setIsActive(false);
        creditPackRepository.save(pack);
    }

    private CreditPack findPackOrThrow(String packId) {
        return creditPackRepository.findById(packId)
                .orElseThrow(() -> new NotFoundException("Credit pack not found: " + packId));
    }

    private CreditPackData toPackData(CreditPack pack) {
        BigDecimal costPerCredit = BigDecimal.ZERO;
        if (pack.getCreditsIncluded() > 0 && pack.getPriceInCents() > 0) {
            costPerCredit = BigDecimal.valueOf(pack.getPriceInCents())
                    .divide(BigDecimal.valueOf(pack.getCreditsIncluded()), 2, RoundingMode.HALF_UP);
        }

        return CreditPackData.builder()
                .id(pack.getId())
                .name(pack.getName().name())
                .displayName(pack.getDisplayName())
                .priceInCents(pack.getPriceInCents())
                .currency(pack.getCurrency())
                .creditsIncluded(pack.getCreditsIncluded())
                .costPerCredit(costPerCredit)
                .build();
    }

    private CreditPackData toPackDataWithCurrency(CreditPack pack, SubscriptionCurrency targetCurrency) {
        CreditPackData data = toPackData(pack);

        // Convert from cents to dollars for the conversion service
        BigDecimal amountInUsd = BigDecimal.valueOf(pack.getPriceInCents())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        ConvertedPrice converted = currencyConversionService.convert(amountInUsd, targetCurrency);

        // Convert back to cents for the response
        long convertedCents = converted.getConvertedAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        data.setConvertedPriceInCents(convertedCents);
        data.setConvertedCurrency(targetCurrency.getCode());
        data.setFormattedPrice(converted.getFormattedPrice());
        data.setFormattedOriginalPrice(
                currencyConversionService.formatPrice(amountInUsd, SubscriptionCurrency.USD));
        data.setExchangeRate(converted.getExchangeRate());

        return data;
    }
}
