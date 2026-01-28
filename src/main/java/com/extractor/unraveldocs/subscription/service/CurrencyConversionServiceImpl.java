package com.extractor.unraveldocs.subscription.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.subscription.config.CurrencyApiConfig;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.extractor.unraveldocs.subscription.dto.ConvertedPrice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of CurrencyConversionService using exchangerate-api.com.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final CurrencyApiConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitizer;

    // In-memory cache of exchange rates
    private final Map<String, BigDecimal> ratesCache = new ConcurrentHashMap<>();
    private OffsetDateTime lastUpdated;

    // Fallback rates (approximate, updated as of Dec 2024)
    private static final Map<String, BigDecimal> FALLBACK_RATES = Map.ofEntries(
            Map.entry("USD", BigDecimal.ONE),
            Map.entry("EUR", new BigDecimal("0.92")),
            Map.entry("GBP", new BigDecimal("0.79")),
            Map.entry("NGN", new BigDecimal("1550.00")),
            Map.entry("INR", new BigDecimal("83.50")),
            Map.entry("JPY", new BigDecimal("142.00")),
            Map.entry("AUD", new BigDecimal("1.52")),
            Map.entry("CAD", new BigDecimal("1.36")),
            Map.entry("CNY", new BigDecimal("7.15")),
            Map.entry("ZAR", new BigDecimal("18.50")),
            Map.entry("GHS", new BigDecimal("15.50")),
            Map.entry("KES", new BigDecimal("153.00")),
            Map.entry("BRL", new BigDecimal("4.95")),
            Map.entry("MXN", new BigDecimal("17.20")),
            Map.entry("AED", new BigDecimal("3.67")),
            Map.entry("SGD", new BigDecimal("1.34")),
            Map.entry("CHF", new BigDecimal("0.87")),
            Map.entry("SEK", new BigDecimal("10.30")),
            Map.entry("NOK", new BigDecimal("10.50")),
            Map.entry("DKK", new BigDecimal("6.85")),
            Map.entry("PLN", new BigDecimal("3.95")),
            Map.entry("TRY", new BigDecimal("31.00")),
            Map.entry("KRW", new BigDecimal("1320.00")),
            Map.entry("THB", new BigDecimal("35.00")),
            Map.entry("IDR", new BigDecimal("15500.00")),
            Map.entry("MYR", new BigDecimal("4.70")),
            Map.entry("PHP", new BigDecimal("55.50")),
            Map.entry("EGP", new BigDecimal("31.00")),
            Map.entry("PKR", new BigDecimal("280.00")));

    @PostConstruct
    public void init() {
        // Initialize with fallback rates
        ratesCache.putAll(FALLBACK_RATES);
        lastUpdated = OffsetDateTime.now();
        log.info("Initialized currency conversion service with fallback rates");
    }

    @Override
    public ConvertedPrice convert(BigDecimal amountUsd, SubscriptionCurrency targetCurrency) {
        if (targetCurrency == SubscriptionCurrency.USD) {
            return ConvertedPrice.builder()
                    .originalAmountUsd(amountUsd)
                    .convertedAmount(amountUsd)
                    .currency(targetCurrency)
                    .formattedPrice(formatPrice(amountUsd, targetCurrency))
                    .exchangeRate(BigDecimal.ONE)
                    .rateTimestamp(lastUpdated)
                    .build();
        }

        BigDecimal rate = getExchangeRate(targetCurrency);
        BigDecimal convertedAmount = amountUsd.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        return ConvertedPrice.builder()
                .originalAmountUsd(amountUsd)
                .convertedAmount(convertedAmount)
                .currency(targetCurrency)
                .formattedPrice(formatPrice(convertedAmount, targetCurrency))
                .exchangeRate(rate)
                .rateTimestamp(lastUpdated)
                .build();
    }

    @Override
    public BigDecimal getExchangeRate(SubscriptionCurrency currency) {
        String code = currency.getCode();

        BigDecimal rate = ratesCache.get(code);
        if (rate != null) {
            return rate;
        }

        // Fallback if not in cache
        rate = FALLBACK_RATES.getOrDefault(code, BigDecimal.ONE);
        log.warn("Exchange rate for {} not found in cache, using fallback: {}", code, rate);
        return rate;
    }

    @Override
    @CacheEvict(value = "currencyConversion", allEntries = true)
    public void refreshRates() {
        log.info("Refreshing exchange rates from API...");

        if (config.getKey() == null || config.getKey().trim().isEmpty()) {
            log.warn("No API key configured for exchange rate API, using fallback rates");
            return;
        }

        try {
            String url = String.format("%s/%s/latest/USD", config.getBaseUrl(), config.getKey());
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                if ("success".equals(root.path("result").asText())) {
                    JsonNode conversionRates = root.path("conversion_rates");

                    for (SubscriptionCurrency currency : SubscriptionCurrency.values()) {
                        String code = currency.getCode();
                        if (conversionRates.has(code)) {
                            try {
                                String rateText = conversionRates.get(code).asText();
                                BigDecimal rate = new BigDecimal(rateText);
                                if (rate.compareTo(BigDecimal.ZERO) > 0) {
                                    ratesCache.put(code, rate);
                                } else {
                                    log.warn("Invalid exchange rate for {}: {}", sanitizer.sanitizeLogging(code), sanitizer.sanitizeLogging(rateText));
                                }
                            } catch (NumberFormatException nfe) {
                                log.warn("Failed to parse exchange rate for {}: {}", sanitizer.sanitizeLogging(code), nfe.getMessage());
                            }
                        }
                    }

                    lastUpdated = OffsetDateTime.now();
                    log.info("Successfully refreshed {} exchange rates", ratesCache.size());
                } else {
                    log.error("API returned error: {}", root.path("error-type").asText());
                }
            }
        } catch (Exception e) {
            log.error("Failed to refresh exchange rates: {}", e.getMessage(), e);
        }
    }

    @Override
    public String formatPrice(BigDecimal amount, SubscriptionCurrency currency) {
        try {
            Currency javaCurrency = Currency.getInstance(currency.getCode());
            Locale locale = getLocaleForCurrency(currency);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
            formatter.setCurrency(javaCurrency);
            return formatter.format(amount);
        } catch (Exception e) {
            // Fallback to simple formatting
            return currency.getSymbol() + String.format("%,.2f", amount);
        }
    }

    private Locale getLocaleForCurrency(SubscriptionCurrency currency) {
        return switch (currency) {
            case GBP -> Locale.UK;
            case EUR -> Locale.GERMANY;
            case JPY -> Locale.JAPAN;
            case CNY -> Locale.CHINA;
            case INR -> new Locale.Builder().setLanguage("en").setRegion("IN").build();
            case NGN -> new Locale.Builder().setLanguage("en").setRegion("NG").build();
            case ZAR -> new Locale.Builder().setLanguage("en").setRegion("ZA").build();
            case BRL -> new Locale.Builder().setLanguage("pt").setRegion("BR").build();
            case MXN -> new Locale.Builder().setLanguage("es").setRegion("MX").build();
            case CAD -> Locale.CANADA;
            case AUD -> new Locale.Builder().setLanguage("en").setRegion("AU").build();
            case RUB -> new Locale.Builder().setLanguage("ru").setRegion("RU").build();
            case AED -> new Locale.Builder().setLanguage("ar").setRegion("AE").build();
            case SGD -> new Locale.Builder().setLanguage("en").setRegion("SG").build();
            case CHF -> new Locale.Builder().setLanguage("de").setRegion("CH").build();
            case SEK -> new Locale.Builder().setLanguage("sv").setRegion("SE").build();
            case NOK -> new Locale.Builder().setLanguage("no").setRegion("NO").build();
            case DKK -> new Locale.Builder().setLanguage("da").setRegion("DK").build();
            case TRY -> new Locale.Builder().setLanguage("tr").setRegion("TR").build();
            case KRW -> new Locale.Builder().setLanguage("ko").setRegion("KR").build();
            case THB -> new Locale.Builder().setLanguage("th").setRegion("TH").build();
            case IDR -> new Locale.Builder().setLanguage("id").setRegion("ID").build();
            case MYR -> new Locale.Builder().setLanguage("ms").setRegion("MY").build();
            case PHP -> new Locale.Builder().setLanguage("en").setRegion("PH").build();
            case EGP -> new Locale.Builder().setLanguage("ar").setRegion("EG").build();
            case PKR -> new Locale.Builder().setLanguage("en").setRegion("PK").build();
            case NZD -> new Locale.Builder().setLanguage("en").setRegion("NZ").build();
            case HKD -> new Locale.Builder().setLanguage("zh").setRegion("HK").build();
            case PLN -> new Locale.Builder().setLanguage("pl").setRegion("PL").build();
            case VND -> new Locale.Builder().setLanguage("vi").setRegion("VN").build();
            case ARS -> new Locale.Builder().setLanguage("es").setRegion("AR").build();
            case CLP -> new Locale.Builder().setLanguage("es").setRegion("CL").build();
            case COP -> new Locale.Builder().setLanguage("es").setRegion("CO").build();
            case PEN -> new Locale.Builder().setLanguage("es").setRegion("PE").build();
            case ILS -> new Locale.Builder().setLanguage("he").setRegion("IL").build();
            case KZT -> new Locale.Builder().setLanguage("kk").setRegion("KZ").build();
            case UAH -> new Locale.Builder().setLanguage("uk").setRegion("UA").build();
            case RON -> new Locale.Builder().setLanguage("ro").setRegion("RO").build();
            case HUF -> new Locale.Builder().setLanguage("hu").setRegion("HU").build();
            case CZK -> new Locale.Builder().setLanguage("cs").setRegion("CZ").build();
            case BGN -> new Locale.Builder().setLanguage("bg").setRegion("BG").build();
            case HRK -> new Locale.Builder().setLanguage("hr").setRegion("HR").build();
            case ISK -> new Locale.Builder().setLanguage("is").setRegion("IS").build();
            case LTL -> new Locale.Builder().setLanguage("lt").setRegion("LT").build();
            case LVL -> new Locale.Builder().setLanguage("lv").setRegion("LV").build();
            case MAD -> new Locale.Builder().setLanguage("ar").setRegion("MA").build();
            case JMD -> new Locale.Builder().setLanguage("en").setRegion("JM").build();
            case BDT -> new Locale.Builder().setLanguage("bn").setRegion("BD").build();
            case TWD -> new Locale.Builder().setLanguage("zh").setRegion("TW").build();
            case ZMW -> new Locale.Builder().setLanguage("en").setRegion("ZM").build();
            case KES -> new Locale.Builder().setLanguage("en").setRegion("KE").build();
            case GHS -> new Locale.Builder().setLanguage("en").setRegion("GH").build();
            case LKR -> new Locale.Builder().setLanguage("si").setRegion("LK").build();
            case MUR -> new Locale.Builder().setLanguage("ms").setRegion("MU").build();
            case TND -> new Locale.Builder().setLanguage("ar").setRegion("TN").build();
            case OMR -> new Locale.Builder().setLanguage("ar").setRegion("OM").build();
            case QAR -> new Locale.Builder().setLanguage("ar").setRegion("QA").build();
            case KWD -> new Locale.Builder().setLanguage("ar").setRegion("KW").build();
            case BHD -> new Locale.Builder().setLanguage("ar").setRegion("BH").build();
            case JOD -> new Locale.Builder().setLanguage("ar").setRegion("JO").build();
            default -> Locale.US;
        };
    }
}
