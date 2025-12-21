package com.extractor.unraveldocs.payment.common.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration optimized for high-throughput payment processing.
 *
 * <h2>Design for Thousands of TPS:</h2>
 * <ul>
 *   <li><b>High Partition Count:</b> 12 partitions allow for parallel processing
 *       by multiple consumers. For very high throughput, increase to 24-48.</li>
 *   <li><b>Retention:</b> 30 days for audit/compliance, with compaction for state topics.</li>
 *   <li><b>Separate Topics by Purpose:</b> Different topics for events, webhooks,
 *       and notifications to allow independent scaling.</li>
 * </ul>
 *
 * <h2>Scaling Recommendations:</h2>
 * <ul>
 *   <li>For 1,000 TPS: 12 partitions, 3-4 consumer instances</li>
 *   <li>For 5,000 TPS: 24 partitions, 8-12 consumer instances</li>
 *   <li>For 10,000+ TPS: 48 partitions, 16+ consumer instances</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class PaymentTopicConfig {

    // ==================== Main Payment Topics ====================

    /**
     * Main topic for all payment events (succeeded, failed, refunded, etc.)
     */
    public static final String TOPIC_PAYMENT_EVENTS = "unraveldocs-payment-events";

    /**
     * Topic for raw webhook events from all payment providers.
     * High throughput topic - receives webhooks from Stripe, Paystack, etc.
     */
    public static final String TOPIC_PAYMENT_WEBHOOKS = "unraveldocs-payment-webhooks";

    /**
     * Topic for subscription lifecycle events.
     */
    public static final String TOPIC_SUBSCRIPTION_EVENTS = "unraveldocs-subscription-events";

    /**
     * Topic for payment notifications (emails, push notifications, etc.)
     */
    public static final String TOPIC_PAYMENT_NOTIFICATIONS = "unraveldocs-payment-notifications";

    // ==================== Retry Topics ====================

    public static final String TOPIC_PAYMENT_EVENTS_RETRY = "unraveldocs-payment-events-retry";
    public static final String TOPIC_PAYMENT_WEBHOOKS_RETRY = "unraveldocs-payment-webhooks-retry";

    // ==================== Dead Letter Queue Topics ====================

    public static final String TOPIC_PAYMENT_EVENTS_DLQ = "unraveldocs-payment-events-dlq";
    public static final String TOPIC_PAYMENT_WEBHOOKS_DLQ = "unraveldocs-payment-webhooks-dlq";

    // ==================== Partition Configuration ====================

    /**
     * Partition count for high-throughput payment topics.
     * 12 partitions support ~3,000-5,000 TPS with proper consumer scaling.
     * Increase to 24-48 for higher throughput requirements.
     */
    private static final int HIGH_THROUGHPUT_PARTITIONS = 12;

    /**
     * Partition count for lower-volume topics.
     */
    private static final int STANDARD_PARTITIONS = 6;

    /**
     * Partition count for DLQ/retry topics.
     */
    private static final int DLQ_PARTITIONS = 3;

    // ==================== Retention Configuration ====================

    private static final String RETENTION_30_DAYS = "2592000000";   // 30 days
    private static final String RETENTION_7_DAYS = "604800000";     // 7 days
    private static final String RETENTION_1_DAY = "86400000";       // 1 day
    private static final String RETENTION_90_DAYS = "7776000000";   // 90 days (for audit)

    // ==================== Topic Beans ====================

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_EVENTS)
                .partitions(HIGH_THROUGHPUT_PARTITIONS)
                .replicas(1) // Set to 3 in production for HA
                .config("retention.ms", RETENTION_30_DAYS)
                .config("cleanup.policy", "delete")
                // Optimize for throughput
                .config("min.insync.replicas", "1") // Set to 2 in production
                .config("segment.bytes", "1073741824") // 1GB segments
                .build();
    }

    @Bean
    public NewTopic paymentWebhooksTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_WEBHOOKS)
                .partitions(HIGH_THROUGHPUT_PARTITIONS)
                .replicas(1)
                .config("retention.ms", RETENTION_7_DAYS)
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic subscriptionEventsTopic() {
        return TopicBuilder.name(TOPIC_SUBSCRIPTION_EVENTS)
                .partitions(STANDARD_PARTITIONS)
                .replicas(1)
                .config("retention.ms", RETENTION_30_DAYS)
                .build();
    }

    @Bean
    public NewTopic paymentNotificationsTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_NOTIFICATIONS)
                .partitions(STANDARD_PARTITIONS)
                .replicas(1)
                .config("retention.ms", RETENTION_7_DAYS)
                .build();
    }

    // ==================== Retry Topics ====================

    @Bean
    public NewTopic paymentEventsRetryTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_EVENTS_RETRY)
                .partitions(DLQ_PARTITIONS)
                .replicas(1)
                .config("retention.ms", RETENTION_1_DAY)
                .build();
    }

    @Bean
    public NewTopic paymentWebhooksRetryTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_WEBHOOKS_RETRY)
                .partitions(DLQ_PARTITIONS)
                .replicas(1)
                .config("retention.ms", RETENTION_1_DAY)
                .build();
    }

    // ==================== Dead Letter Queue Topics ====================

    @Bean
    public NewTopic paymentEventsDlqTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_EVENTS_DLQ)
                .partitions(DLQ_PARTITIONS)
                .replicas(1)
                .config("retention.ms", RETENTION_90_DAYS) // Long retention for investigation
                .build();
    }

    @Bean
    public NewTopic paymentWebhooksDlqTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_WEBHOOKS_DLQ)
                .partitions(DLQ_PARTITIONS)
                .replicas(1)
                .config("retention.ms", RETENTION_90_DAYS)
                .build();
    }
}

