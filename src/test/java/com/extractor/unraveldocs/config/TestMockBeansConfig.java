package com.extractor.unraveldocs.config;

import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.events.ReceiptEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration that provides mock beans for optional dependencies
 * that are conditionally created based on external services (Kafka, etc.).
 */
@Configuration
public class TestMockBeansConfig {

    /**
     * Provides a no-op mock ReceiptEventPublisher when Kafka is not available.
     * The real bean is @ConditionalOnProperty(name =
     * "spring.kafka.bootstrap-servers"),
     * so in tests without Kafka, this mock is used instead.
     */
    @Bean
    @ConditionalOnMissingBean(ReceiptEventPublisher.class)
    public ReceiptEventPublisher receiptEventPublisher() {
        return new ReceiptEventPublisher(null, null) {
            @Override
            public void publishReceiptRequest(ReceiptData receiptData) {
                // No-op for tests
            }
        };
    }
}
