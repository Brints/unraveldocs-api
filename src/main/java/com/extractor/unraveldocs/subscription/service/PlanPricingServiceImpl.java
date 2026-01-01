package com.extractor.unraveldocs.subscription.service;

import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;
import com.extractor.unraveldocs.subscription.dto.response.*;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.team.model.TeamSubscriptionPlan;
import com.extractor.unraveldocs.team.repository.TeamSubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of PlanPricingService for retrieving subscription plans with
 * currency conversion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPricingServiceImpl implements PlanPricingService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TeamSubscriptionPlanRepository teamSubscriptionPlanRepository;
    private final CurrencyConversionService currencyConversionService;

    @Override
    public AllPlansWithPricingResponse getAllPlansWithPricing(SubscriptionCurrency currency) {
        log.info("Fetching all plans with pricing in currency: {}", currency.getCode());

        // Fetch all active individual plans
        List<SubscriptionPlan> individualPlans = subscriptionPlanRepository.findAll()
                .stream()
                .filter(SubscriptionPlan::isActive)
                .toList();

        // Fetch all active team plans
        List<TeamSubscriptionPlan> teamPlans = teamSubscriptionPlanRepository.findAllActive();

        // Convert individual plans
        List<IndividualPlanPricingDto> individualPlanDtos = individualPlans.stream()
                .map(plan -> convertIndividualPlan(plan, currency))
                .collect(Collectors.toList());

        // Convert team plans
        List<TeamPlanPricingDto> teamPlanDtos = teamPlans.stream()
                .map(plan -> convertTeamPlan(plan, currency))
                .collect(Collectors.toList());

        // Get the exchange rate timestamp
        ConvertedPrice sampleConversion = currencyConversionService.convert(
                java.math.BigDecimal.ONE, currency);

        return AllPlansWithPricingResponse.builder()
                .individualPlans(individualPlanDtos)
                .teamPlans(teamPlanDtos)
                .displayCurrency(currency)
                .exchangeRateTimestamp(sampleConversion.getRateTimestamp())
                .build();
    }

    @Override
    public SupportedCurrenciesResponse getSupportedCurrencies() {
        log.info("Fetching list of supported currencies");

        List<CurrencyInfo> currencies = Arrays.stream(SubscriptionCurrency.values())
                .map(currency -> CurrencyInfo.builder()
                        .code(currency.getCode())
                        .symbol(currency.getSymbol())
                        .name(currency.getFullName())
                        .build())
                .collect(Collectors.toList());

        return SupportedCurrenciesResponse.builder()
                .currencies(currencies)
                .totalCount(currencies.size())
                .build();
    }

    /**
     * Convert an individual subscription plan to DTO with converted pricing.
     */
    private IndividualPlanPricingDto convertIndividualPlan(SubscriptionPlan plan, SubscriptionCurrency currency) {
        ConvertedPrice convertedPrice = currencyConversionService.convert(plan.getPrice(), currency);

        return IndividualPlanPricingDto.builder()
                .planId(plan.getId())
                .planName(plan.getName().name())
                .displayName(formatPlanDisplayName(plan.getName().name()))
                .billingInterval(plan.getBillingIntervalUnit().name())
                .price(convertedPrice)
                .documentUploadLimit(plan.getDocumentUploadLimit())
                .ocrPageLimit(plan.getOcrPageLimit())
                .isActive(plan.isActive())
                .features(getPlanFeatures(plan.getName().name()))
                .build();
    }

    /**
     * Convert a team subscription plan to DTO with converted pricing.
     */
    private TeamPlanPricingDto convertTeamPlan(TeamSubscriptionPlan plan, SubscriptionCurrency currency) {
        ConvertedPrice monthlyConvertedPrice = currencyConversionService.convert(
                plan.getMonthlyPrice(), currency);
        ConvertedPrice yearlyConvertedPrice = currencyConversionService.convert(
                plan.getYearlyPrice(), currency);

        return TeamPlanPricingDto.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .displayName(plan.getDisplayName())
                .description(plan.getDescription())
                .monthlyPrice(monthlyConvertedPrice)
                .yearlyPrice(yearlyConvertedPrice)
                .maxMembers(plan.getMaxMembers())
                .monthlyDocumentLimit(plan.getMonthlyDocumentLimit())
                .hasAdminPromotion(plan.isHasAdminPromotion())
                .hasEmailInvitations(plan.isHasEmailInvitations())
                .trialDays(plan.getTrialDays())
                .isActive(plan.isActive())
                .features(getTeamPlanFeatures(plan))
                .build();
    }

    /**
     * Format plan name for display (e.g., "PRO_MONTHLY" -> "Pro Monthly").
     */
    private String formatPlanDisplayName(String planName) {
        return Arrays.stream(planName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Get list of features for an individual plan based on plan name.
     */
    private List<String> getPlanFeatures(String planName) {
        List<String> features = new ArrayList<>();

        if (planName.contains("FREE")) {
            features.add("Basic document processing");
            features.add("Limited OCR pages");
            features.add("Email support");
        } else if (planName.contains("STARTER")) {
            features.add("Standard document processing");
            features.add("Increased OCR pages");
            features.add("Priority email support");
            features.add("API access");
        } else if (planName.contains("PRO")) {
            features.add("Advanced document processing");
            features.add("High OCR page limit");
            features.add("Priority support");
            features.add("Full API access");
            features.add("Custom integrations");
        } else if (planName.contains("BUSINESS") || planName.contains("ENTERPRISE")) {
            features.add("Unlimited document processing");
            features.add("Unlimited OCR pages");
            features.add("24/7 premium support");
            features.add("Full API access");
            features.add("Custom integrations");
            features.add("Dedicated account manager");
        }

        return features;
    }

    /**
     * Get list of features for a team plan.
     */
    private List<String> getTeamPlanFeatures(TeamSubscriptionPlan plan) {
        List<String> features = new ArrayList<>();

        features.add("Up to " + plan.getMaxMembers() + " team members");

        if (plan.hasUnlimitedDocuments()) {
            features.add("Unlimited documents");
        } else {
            features.add(plan.getMonthlyDocumentLimit() + " documents per month");
        }

        if (plan.isHasAdminPromotion()) {
            features.add("Admin role promotion");
        }

        if (plan.isHasEmailInvitations()) {
            features.add("Email invitations");
        }

        if (plan.getTrialDays() > 0) {
            features.add(plan.getTrialDays() + "-day free trial");
        }

        features.add("Team collaboration");
        features.add("Shared workspace");

        return features;
    }
}
