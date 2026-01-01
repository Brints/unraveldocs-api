package com.extractor.unraveldocs.subscription.service.impl;

import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;
import com.extractor.unraveldocs.subscription.dto.response.*;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.service.CurrencyConversionService;
import com.extractor.unraveldocs.subscription.service.PlanPricingServiceImpl;
import com.extractor.unraveldocs.team.model.TeamSubscriptionPlan;
import com.extractor.unraveldocs.team.repository.TeamSubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanPricingServiceImpl Tests")
class PlanPricingServiceImplTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private TeamSubscriptionPlanRepository teamSubscriptionPlanRepository;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @InjectMocks
    private PlanPricingServiceImpl planPricingService;

    private SubscriptionPlan individualPlan;
    private TeamSubscriptionPlan teamPlan;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now();

        // Setup individual plan
        individualPlan = new SubscriptionPlan();
        individualPlan.setId("plan-1");
        individualPlan.setName(SubscriptionPlans.PRO_MONTHLY);
        individualPlan.setPrice(new BigDecimal("29.99"));
        individualPlan.setCurrency(SubscriptionCurrency.USD);
        individualPlan.setBillingIntervalUnit(BillingIntervalUnit.MONTH);
        individualPlan.setBillingIntervalValue(1);
        individualPlan.setDocumentUploadLimit(500);
        individualPlan.setOcrPageLimit(2000);
        individualPlan.setActive(true);
        individualPlan.setCreatedAt(now);
        individualPlan.setUpdatedAt(now);

        // Setup team plan
        teamPlan = new TeamSubscriptionPlan();
        teamPlan.setId("team-plan-1");
        teamPlan.setName("TEAM_PREMIUM");
        teamPlan.setDisplayName("Team Premium");
        teamPlan.setDescription("Premium team plan");
        teamPlan.setMonthlyPrice(new BigDecimal("99.00"));
        teamPlan.setYearlyPrice(new BigDecimal("990.00"));
        teamPlan.setCurrency("USD");
        teamPlan.setMaxMembers(10);
        teamPlan.setMonthlyDocumentLimit(1000);
        teamPlan.setHasAdminPromotion(true);
        teamPlan.setHasEmailInvitations(true);
        teamPlan.setTrialDays(14);
        teamPlan.setActive(true);
    }

    @Nested
    @DisplayName("getAllPlansWithPricing Tests")
    class GetAllPlansWithPricingTests {

        @Test
        @DisplayName("Should return plans with USD pricing when USD currency is specified")
        void shouldReturnPlansWithUsdPricing() {
            // Arrange
            when(subscriptionPlanRepository.findAll()).thenReturn(List.of(individualPlan));
            when(teamSubscriptionPlanRepository.findAllActive()).thenReturn(List.of(teamPlan));

            ConvertedPrice usdConvertedPrice = ConvertedPrice.builder()
                    .originalAmountUsd(new BigDecimal("29.99"))
                    .convertedAmount(new BigDecimal("29.99"))
                    .currency(SubscriptionCurrency.USD)
                    .formattedPrice("$29.99")
                    .exchangeRate(BigDecimal.ONE)
                    .rateTimestamp(now)
                    .build();

            when(currencyConversionService.convert(any(BigDecimal.class), eq(SubscriptionCurrency.USD)))
                    .thenReturn(usdConvertedPrice);

            // Act
            AllPlansWithPricingResponse response = planPricingService.getAllPlansWithPricing(SubscriptionCurrency.USD);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getDisplayCurrency()).isEqualTo(SubscriptionCurrency.USD);
            assertThat(response.getIndividualPlans()).hasSize(1);
            assertThat(response.getTeamPlans()).hasSize(1);

            IndividualPlanPricingDto individualDto = response.getIndividualPlans().get(0);
            assertThat(individualDto.getPlanId()).isEqualTo("plan-1");
            assertThat(individualDto.getPlanName()).isEqualTo("PRO_MONTHLY");
            assertThat(individualDto.getPrice().getFormattedPrice()).isEqualTo("$29.99");
        }

        @Test
        @DisplayName("Should return plans with NGN pricing when NGN currency is specified")
        void shouldReturnPlansWithNgnPricing() {
            // Arrange
            when(subscriptionPlanRepository.findAll()).thenReturn(List.of(individualPlan));
            when(teamSubscriptionPlanRepository.findAllActive()).thenReturn(List.of(teamPlan));

            BigDecimal ngnRate = new BigDecimal("1550.00");
            ConvertedPrice ngnConvertedPrice = ConvertedPrice.builder()
                    .originalAmountUsd(new BigDecimal("29.99"))
                    .convertedAmount(new BigDecimal("46484.50"))
                    .currency(SubscriptionCurrency.NGN)
                    .formattedPrice("₦46,484.50")
                    .exchangeRate(ngnRate)
                    .rateTimestamp(now)
                    .build();

            when(currencyConversionService.convert(any(BigDecimal.class), eq(SubscriptionCurrency.NGN)))
                    .thenReturn(ngnConvertedPrice);

            // Act
            AllPlansWithPricingResponse response = planPricingService.getAllPlansWithPricing(SubscriptionCurrency.NGN);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getDisplayCurrency()).isEqualTo(SubscriptionCurrency.NGN);

            IndividualPlanPricingDto individualDto = response.getIndividualPlans().get(0);
            assertThat(individualDto.getPrice().getCurrency()).isEqualTo(SubscriptionCurrency.NGN);
            assertThat(individualDto.getPrice().getExchangeRate()).isEqualTo(ngnRate);
        }

        @Test
        @DisplayName("Should return empty lists when no plans exist")
        void shouldReturnEmptyListsWhenNoPlansExist() {
            // Arrange
            when(subscriptionPlanRepository.findAll()).thenReturn(Collections.emptyList());
            when(teamSubscriptionPlanRepository.findAllActive()).thenReturn(Collections.emptyList());

            ConvertedPrice usdConvertedPrice = ConvertedPrice.builder()
                    .originalAmountUsd(BigDecimal.ONE)
                    .convertedAmount(BigDecimal.ONE)
                    .currency(SubscriptionCurrency.USD)
                    .formattedPrice("$1.00")
                    .exchangeRate(BigDecimal.ONE)
                    .rateTimestamp(now)
                    .build();

            when(currencyConversionService.convert(any(BigDecimal.class), eq(SubscriptionCurrency.USD)))
                    .thenReturn(usdConvertedPrice);

            // Act
            AllPlansWithPricingResponse response = planPricingService.getAllPlansWithPricing(SubscriptionCurrency.USD);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getIndividualPlans()).isEmpty();
            assertThat(response.getTeamPlans()).isEmpty();
            assertThat(response.getDisplayCurrency()).isEqualTo(SubscriptionCurrency.USD);
        }

        @Test
        @DisplayName("Should only return active plans")
        void shouldOnlyReturnActivePlans() {
            // Arrange
            SubscriptionPlan inactivePlan = new SubscriptionPlan();
            inactivePlan.setId("inactive-plan");
            inactivePlan.setName(SubscriptionPlans.FREE);
            inactivePlan.setPrice(BigDecimal.ZERO);
            inactivePlan.setActive(false);

            when(subscriptionPlanRepository.findAll()).thenReturn(List.of(individualPlan, inactivePlan));
            when(teamSubscriptionPlanRepository.findAllActive()).thenReturn(Collections.emptyList());

            ConvertedPrice usdConvertedPrice = ConvertedPrice.builder()
                    .originalAmountUsd(new BigDecimal("29.99"))
                    .convertedAmount(new BigDecimal("29.99"))
                    .currency(SubscriptionCurrency.USD)
                    .formattedPrice("$29.99")
                    .exchangeRate(BigDecimal.ONE)
                    .rateTimestamp(now)
                    .build();

            when(currencyConversionService.convert(any(BigDecimal.class), eq(SubscriptionCurrency.USD)))
                    .thenReturn(usdConvertedPrice);

            // Act
            AllPlansWithPricingResponse response = planPricingService.getAllPlansWithPricing(SubscriptionCurrency.USD);

            // Assert
            assertThat(response.getIndividualPlans()).hasSize(1);
            assertThat(response.getIndividualPlans().get(0).getPlanId()).isEqualTo("plan-1");
        }

        @Test
        @DisplayName("Should include features for individual plans")
        void shouldIncludeFeaturesForIndividualPlans() {
            // Arrange
            when(subscriptionPlanRepository.findAll()).thenReturn(List.of(individualPlan));
            when(teamSubscriptionPlanRepository.findAllActive()).thenReturn(Collections.emptyList());

            ConvertedPrice usdConvertedPrice = ConvertedPrice.builder()
                    .originalAmountUsd(new BigDecimal("29.99"))
                    .convertedAmount(new BigDecimal("29.99"))
                    .currency(SubscriptionCurrency.USD)
                    .formattedPrice("$29.99")
                    .exchangeRate(BigDecimal.ONE)
                    .rateTimestamp(now)
                    .build();

            when(currencyConversionService.convert(any(BigDecimal.class), eq(SubscriptionCurrency.USD)))
                    .thenReturn(usdConvertedPrice);

            // Act
            AllPlansWithPricingResponse response = planPricingService.getAllPlansWithPricing(SubscriptionCurrency.USD);

            // Assert
            IndividualPlanPricingDto dto = response.getIndividualPlans().get(0);
            assertThat(dto.getFeatures()).isNotEmpty();
            assertThat(dto.getFeatures()).contains("Advanced document processing");
        }

        @Test
        @DisplayName("Should include features for team plans")
        void shouldIncludeFeaturesForTeamPlans() {
            // Arrange
            when(subscriptionPlanRepository.findAll()).thenReturn(Collections.emptyList());
            when(teamSubscriptionPlanRepository.findAllActive()).thenReturn(List.of(teamPlan));

            ConvertedPrice usdConvertedPrice = ConvertedPrice.builder()
                    .originalAmountUsd(new BigDecimal("99.00"))
                    .convertedAmount(new BigDecimal("99.00"))
                    .currency(SubscriptionCurrency.USD)
                    .formattedPrice("$99.00")
                    .exchangeRate(BigDecimal.ONE)
                    .rateTimestamp(now)
                    .build();

            when(currencyConversionService.convert(any(BigDecimal.class), eq(SubscriptionCurrency.USD)))
                    .thenReturn(usdConvertedPrice);

            // Act
            AllPlansWithPricingResponse response = planPricingService.getAllPlansWithPricing(SubscriptionCurrency.USD);

            // Assert
            TeamPlanPricingDto dto = response.getTeamPlans().get(0);
            assertThat(dto.getFeatures()).isNotEmpty();
            assertThat(dto.getFeatures()).contains("Up to 10 team members");
            assertThat(dto.getFeatures()).contains("Admin role promotion");
            assertThat(dto.getFeatures()).contains("Email invitations");
            assertThat(dto.getFeatures()).contains("14-day free trial");
        }
    }

    @Nested
    @DisplayName("getSupportedCurrencies Tests")
    class GetSupportedCurrenciesTests {

        @Test
        @DisplayName("Should return all supported currencies")
        void shouldReturnAllSupportedCurrencies() {
            // Act
            SupportedCurrenciesResponse response = planPricingService.getSupportedCurrencies();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getCurrencies()).isNotEmpty();
            assertThat(response.getTotalCount()).isEqualTo(SubscriptionCurrency.values().length);
        }

        @Test
        @DisplayName("Should include USD currency in the list")
        void shouldIncludeUsdCurrency() {
            // Act
            SupportedCurrenciesResponse response = planPricingService.getSupportedCurrencies();

            // Assert
            boolean hasUsd = response.getCurrencies().stream()
                    .anyMatch(c -> "USD".equals(c.getCode()));
            assertThat(hasUsd).isTrue();
        }

        @Test
        @DisplayName("Should include NGN currency with correct symbol")
        void shouldIncludeNgnCurrencyWithCorrectSymbol() {
            // Act
            SupportedCurrenciesResponse response = planPricingService.getSupportedCurrencies();

            // Assert
            CurrencyInfo ngnCurrency = response.getCurrencies().stream()
                    .filter(c -> "NGN".equals(c.getCode()))
                    .findFirst()
                    .orElse(null);

            assertThat(ngnCurrency).isNotNull();
            assertThat(ngnCurrency.getSymbol()).isEqualTo("₦");
            assertThat(ngnCurrency.getName()).isEqualTo("Nigerian Naira");
        }

        @Test
        @DisplayName("Should return currencies with all required fields")
        void shouldReturnCurrenciesWithAllRequiredFields() {
            // Act
            SupportedCurrenciesResponse response = planPricingService.getSupportedCurrencies();

            // Assert
            for (CurrencyInfo currency : response.getCurrencies()) {
                assertThat(currency.getCode()).isNotBlank();
                assertThat(currency.getSymbol()).isNotBlank();
                assertThat(currency.getName()).isNotBlank();
            }
        }
    }
}
