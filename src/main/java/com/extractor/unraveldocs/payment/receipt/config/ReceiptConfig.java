package com.extractor.unraveldocs.payment.receipt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for receipt generation
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.receipt")
public class ReceiptConfig {

    private String companyName = "UnravelDocs";
    private String companyAddress;
    private String companyEmail;
    private String companyPhone;
    private String logoUrl;
    private String receiptPrefix = "RCP";
    private String defaultCurrency = "USD";
}
