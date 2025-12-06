package com.extractor.unraveldocs.payment.stripe.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Stripe SDK initialization
 */
@Slf4j
@Configuration
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
        log.info("Stripe SDK initialized successfully");
    }
}
