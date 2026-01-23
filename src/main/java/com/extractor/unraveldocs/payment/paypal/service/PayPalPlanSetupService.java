package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.paypal.config.PayPalConfig;
import com.extractor.unraveldocs.payment.paypal.exception.PayPalPaymentException;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for setting up PayPal products and billing plans.
 * This should be run once to create all subscription plans in PayPal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalPlanSetupService {

    private final PayPalConfig payPalConfig;
    private final PayPalAuthService authService;
    private final SubscriptionPlanRepository planRepository;
    private final RestClient paypalRestClient;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitizer;

    private static final String PRODUCT_NAME = "UnravelDocs Subscription";
    private static final String PRODUCT_DESCRIPTION = "Document processing and OCR subscription service";
    private static final String PRODUCT_TYPE = "SERVICE";
    private static final String PRODUCT_CATEGORY = "SOFTWARE";

    /**
     * Set up all PayPal products and plans.
     * Creates one product and multiple billing plans for each subscription tier.
     * 
     * @return Map containing the setup results
     */
    @Transactional
    public Map<String, Object> setupAllPlans() {
        log.info("Starting PayPal plan setup...");

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> createdPlans = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Step 1: Create or get the product
            String productId = createOrGetProduct();
            result.put("productId", productId);
            log.info("Using PayPal product: {}", sanitizer.sanitizeLogging(productId));

            // Step 2: Create billing plans for each subscription tier
            List<SubscriptionPlan> plans = planRepository.findAll();

            for (SubscriptionPlan plan : plans) {
                if (plan.getName() == SubscriptionPlans.FREE) {
                    // Skip free plan - no PayPal subscription needed
                    continue;
                }

                try {
                    String planId = createBillingPlan(productId, plan);

                    // Update the plan with PayPal plan ID
                    plan.setPaypalPlanCode(planId);
                    planRepository.save(plan);

                    createdPlans.add(Map.of(
                            "planName", plan.getName().name(),
                            "paypalPlanId", planId,
                            "price", plan.getPrice().toString(),
                            "interval", plan.getBillingIntervalUnit().name()));

                    log.info(
                            "Created PayPal plan for {}: {}",
                            sanitizer.sanitizeLoggingObject(plan.getName()),
                            sanitizer.sanitizeLogging(planId)
                    );

                } catch (Exception e) {
                    String error = "Failed to create plan for " + plan.getName() + ": " + e.getMessage();
                    errors.add(error);
                    log.error(error, e);
                }
            }

            result.put("createdPlans", createdPlans);
            result.put("planCount", createdPlans.size());
            result.put("errors", errors);
            result.put("success", errors.isEmpty());

            log.info(
                    "PayPal plan setup completed. Created {} plans.",
                    sanitizer.sanitizeLoggingInteger(createdPlans.size())
            );

        } catch (Exception e) {
            log.error("Failed to setup PayPal plans: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Create a PayPal product or return existing one.
     */
    private String createOrGetProduct() {
        try {
            // First, try to find an existing product
            String existingProductId = findExistingProduct();
            if (existingProductId != null) {
                log.info("Found existing PayPal product: {}", existingProductId);
                return existingProductId;
            }

            // Create new product - home_url is optional and PayPal rejects localhost URLs
            Map<String, Object> productRequest = getProductRequest();

            String response = paypalRestClient.post()
                    .uri("/v1/catalogs/products")
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(productRequest)
                    .retrieve()
                    .body(String.class);

            JsonNode productJson = objectMapper.readTree(response);
            String productId = productJson.get("id").asText();

            log.info("Created PayPal product: {}", sanitizer.sanitizeLogging(productId));
            return productId;

        } catch (Exception e) {
            log.error("Failed to create PayPal product: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            throw new PayPalPaymentException("Failed to create PayPal product", e);
        }
    }

    private @NonNull Map<String, Object> getProductRequest() {
        Map<String, Object> productRequest = new LinkedHashMap<>();
        productRequest.put("name", PRODUCT_NAME);
        productRequest.put("description", PRODUCT_DESCRIPTION);
        productRequest.put("type", PRODUCT_TYPE);
        productRequest.put("category", PRODUCT_CATEGORY);

        // Only add home_url if it's not localhost (PayPal rejects localhost URLs)
        String returnUrl = payPalConfig.getReturnUrl();
        if (returnUrl != null && !returnUrl.contains("localhost") && !returnUrl.contains("127.0.0.1")) {
            String homeUrl = returnUrl.replace("/api/v1/paypal/return", "");
            productRequest.put("home_url", homeUrl);
        }
        return productRequest;
    }

    /**
     * Find an existing product by name.
     */
    private String findExistingProduct() {
        try {
            String response = paypalRestClient.get()
                    .uri("/v1/catalogs/products?page_size=20")
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .retrieve()
                    .body(String.class);

            JsonNode productsJson = objectMapper.readTree(response);
            if (productsJson.has("products")) {
                for (JsonNode product : productsJson.get("products")) {
                    if (PRODUCT_NAME.equals(product.get("name").asText())) {
                        return product.get("id").asText();
                    }
                }
            }
            return null;

        } catch (Exception e) {
            log.warn("Could not search for existing products: {}", sanitizer.sanitizeLogging(e.getMessage()));
            return null;
        }
    }

    /**
     * Create a PayPal billing plan for a subscription.
     */
    private String createBillingPlan(String productId, SubscriptionPlan plan) {
        try {
            String intervalUnit = mapBillingInterval(plan.getBillingIntervalUnit().name());
            int intervalCount = plan.getBillingIntervalValue();

            Map<String, Object> billingCycle = new LinkedHashMap<>();
            billingCycle.put("frequency", Map.of(
                    "interval_unit", intervalUnit,
                    "interval_count", intervalCount));
            billingCycle.put("tenure_type", "REGULAR");
            billingCycle.put("sequence", 1);
            billingCycle.put("total_cycles", 0); // 0 = infinite
            billingCycle.put("pricing_scheme", Map.of(
                    "fixed_price", Map.of(
                            "value", plan.getPrice().toString(),
                            "currency_code", plan.getCurrency().name())));

            Map<String, Object> paymentPreferences = new LinkedHashMap<>();
            paymentPreferences.put("auto_bill_outstanding", true);
            paymentPreferences.put("setup_fee", Map.of(
                    "value", "0",
                    "currency_code", plan.getCurrency().name()));
            paymentPreferences.put("setup_fee_failure_action", "CONTINUE");
            paymentPreferences.put("payment_failure_threshold", 3);

            Map<String, Object> planRequest = new LinkedHashMap<>();
            planRequest.put("product_id", productId);
            planRequest.put("name", formatPlanName(plan.getName()));
            planRequest.put("description", formatPlanDescription(plan));
            planRequest.put("status", "ACTIVE");
            planRequest.put("billing_cycles", List.of(billingCycle));
            planRequest.put("payment_preferences", paymentPreferences);

            String response = paypalRestClient.post()
                    .uri("/v1/billing/plans")
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(planRequest)
                    .retrieve()
                    .body(String.class);

            JsonNode planJson = objectMapper.readTree(response);
            return planJson.get("id").asText();

        } catch (Exception e) {
            log.error(
                    "Failed to create billing plan for {}: {}",
                    sanitizer.sanitizeLoggingObject(plan.getName()), e.getMessage(), e);
            throw new PayPalPaymentException("Failed to create billing plan", e);
        }
    }

    /**
     * Map our billing interval to PayPal's format.
     */
    private String mapBillingInterval(String interval) {
        return switch (interval.toUpperCase()) {
            case "YEAR" -> "YEAR";
            case "WEEK" -> "WEEK";
            case "DAY" -> "DAY";
            default -> "MONTH";
        };
    }

    /**
     * Format plan name for PayPal.
     */
    private String formatPlanName(SubscriptionPlans plan) {
        return switch (plan) {
            case STARTER_MONTHLY -> "Starter Monthly";
            case STARTER_YEARLY -> "Starter Yearly";
            case PRO_MONTHLY -> "Pro Monthly";
            case PRO_YEARLY -> "Pro Yearly";
            case BUSINESS_MONTHLY -> "Business Monthly";
            case BUSINESS_YEARLY -> "Business Yearly";
            default -> plan.name().replace("_", " ");
        };
    }

    /**
     * Format plan description for PayPal.
     */
    private String formatPlanDescription(SubscriptionPlan plan) {
        StringBuilder desc = new StringBuilder();
        desc.append("UnravelDocs ").append(formatPlanName(plan.getName()));
        desc.append(" - ");
        desc.append(plan.getDocumentUploadLimit()).append(" documents, ");
        desc.append(plan.getOcrPageLimit()).append(" OCR pages");

        if (plan.getBillingIntervalUnit().name().equals("YEAR")) {
            BigDecimal monthlyEquivalent = plan.getPrice().divide(BigDecimal.valueOf(12), 2,
                    java.math.RoundingMode.HALF_UP);
            desc.append(" ($").append(monthlyEquivalent).append("/mo equivalent)");
        }

        return desc.toString();
    }

    /**
     * List all existing PayPal billing plans.
     */
    public List<Map<String, Object>> listExistingPlans() {
        try {
            String response = paypalRestClient.get()
                    .uri("/v1/billing/plans?page_size=20&total_required=true")
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .retrieve()
                    .body(String.class);

            JsonNode plansJson = objectMapper.readTree(response);
            List<Map<String, Object>> plans = new ArrayList<>();

            if (plansJson.has("plans")) {
                for (JsonNode plan : plansJson.get("plans")) {
                    plans.add(Map.of(
                            "id", plan.get("id").asText(),
                            "name", plan.has("name") ? plan.get("name").asText() : "",
                            "status", plan.has("status") ? plan.get("status").asText() : "",
                            "productId", plan.has("product_id") ? plan.get("product_id").asText() : ""));
                }
            }

            return plans;

        } catch (Exception e) {
            log.error("Failed to list PayPal plans: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            throw new PayPalPaymentException("Failed to list PayPal plans", e);
        }
    }

    /**
     * Deactivate a PayPal billing plan.
     */
    public void deactivatePlan(String planId) {
        try {
            paypalRestClient.post()
                    .uri("/v1/billing/plans/{planId}/deactivate", planId)
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity();

            log.info("Deactivated PayPal plan: {}", sanitizer.sanitizeLogging(planId));

        } catch (Exception e) {
            log.error(
                    "Failed to deactivate plan {}: {}",
                    sanitizer.sanitizeLogging(planId),
                    sanitizer.sanitizeLogging(e.getMessage()), e);
            throw new PayPalPaymentException("Failed to deactivate plan", e);
        }
    }
}
