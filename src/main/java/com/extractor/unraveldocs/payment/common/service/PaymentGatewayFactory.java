package com.extractor.unraveldocs.payment.common.service;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Factory for obtaining the appropriate PaymentGatewayService implementation
 * based on the requested payment provider.
 */
@Slf4j
@Component
public class PaymentGatewayFactory {

    private final Map<PaymentGateway, PaymentGatewayService> providers;
    private final PaymentGateway defaultProvider;

    public PaymentGatewayFactory(List<PaymentGatewayService> paymentGatewayServices) {
        this.providers = new EnumMap<>(PaymentGateway.class);

        for (PaymentGatewayService service : paymentGatewayServices) {
            providers.put(service.getProvider(), service);
            log.info("Registered payment gateway: {}", service.getProvider());
        }

        // Determine default provider (prefer Stripe, then Paystack)
        if (providers.containsKey(PaymentGateway.STRIPE)) {
            defaultProvider = PaymentGateway.STRIPE;
        } else if (providers.containsKey(PaymentGateway.PAYSTACK)) {
            defaultProvider = PaymentGateway.PAYSTACK;
        } else if (!providers.isEmpty()) {
            defaultProvider = providers.keySet().iterator().next();
        } else {
            defaultProvider = null;
            log.warn("No payment gateway services registered!");
        }

        log.info("Default payment gateway: {}", defaultProvider);
    }

    /**
     * Get the payment gateway service for a specific provider.
     *
     * @param gateway The payment gateway to get
     * @return The payment gateway service
     * @throws IllegalArgumentException if the gateway is not supported
     */
    public PaymentGatewayService getProvider(PaymentGateway gateway) {
        PaymentGatewayService service = providers.get(gateway);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported payment gateway: " + gateway);
        }
        return service;
    }

    /**
     * Get the payment gateway service if available.
     *
     * @param gateway The payment gateway to get
     * @return Optional containing the service if available
     */
    public Optional<PaymentGatewayService> getProviderIfAvailable(PaymentGateway gateway) {
        return Optional.ofNullable(providers.get(gateway));
    }

    /**
     * Get the default payment gateway service.
     *
     * @return The default payment gateway service
     * @throws IllegalStateException if no default provider is configured
     */
    public PaymentGatewayService getDefaultProvider() {
        if (defaultProvider == null) {
            throw new IllegalStateException("No payment gateway services are registered");
        }
        return providers.get(defaultProvider);
    }

    /**
     * Get the default payment gateway enum.
     *
     * @return The default payment gateway
     */
    public PaymentGateway getDefaultGateway() {
        return defaultProvider;
    }

    /**
     * Check if a gateway is supported.
     *
     * @param gateway The gateway to check
     * @return true if the gateway is supported
     */
    public boolean isSupported(PaymentGateway gateway) {
        return providers.containsKey(gateway);
    }

    /**
     * Get all supported gateways.
     *
     * @return Set of supported gateways
     */
    public Set<PaymentGateway> getSupportedGateways() {
        return providers.keySet();
    }

    /**
     * Get the appropriate provider based on user's region or preferences.
     * This can be extended to implement region-based provider selection.
     *
     * @param userCountry The user's country code (ISO 2-letter)
     * @return The appropriate payment gateway service
     */
    public PaymentGatewayService getProviderForRegion(String userCountry) {
        // African countries typically use Paystack
        if (isAfricanCountry(userCountry) && providers.containsKey(PaymentGateway.PAYSTACK)) {
            return providers.get(PaymentGateway.PAYSTACK);
        }

        // Ethiopian users use Chappa
        if ("ET".equalsIgnoreCase(userCountry) && providers.containsKey(PaymentGateway.CHAPA)) {
            return providers.get(PaymentGateway.CHAPA);
        }

        // Default to Stripe for other regions
        return getDefaultProvider();
    }

    /**
     * Check if a country is in Africa (where Paystack operates).
     */
    private boolean isAfricanCountry(String countryCode) {
        if (countryCode == null) return false;
        // Major Paystack-supported African countries
        return switch (countryCode.toUpperCase()) {
            case "NG", "GH", "ZA", "KE" -> true;
            default -> false;
        };
    }
}
