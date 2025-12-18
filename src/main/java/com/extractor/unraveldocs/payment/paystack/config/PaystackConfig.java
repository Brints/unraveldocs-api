package com.extractor.unraveldocs.payment.paystack.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Slf4j
@Getter
@Configuration
public class PaystackConfig {

    @Value("${paystack.secret.key}")
    private String secretKey;

    @Value("${paystack.initialize.url:https://api.paystack.co/transaction/initialize}")
    private String initializeUrl;

    @Value("${paystack.verify.url:https://api.paystack.co/transaction/verify/}")
    private String verifyUrl;

    @Value("${paystack.base.url:https://api.paystack.co}")
    private String baseUrl;

    @Value("${paystack.webhook.secret:}")
    private String webhookSecret;

    @Value("${paystack.callback.url:http://localhost:8080/api/v1/paystack/callback}")
    private String callbackUrl;

    @Value("${paystack.currency:NGN}")
    private String defaultCurrency;

    @Bean
    public RestClient paystackRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
