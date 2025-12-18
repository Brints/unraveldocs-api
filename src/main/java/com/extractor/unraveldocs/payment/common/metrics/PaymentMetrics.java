package com.extractor.unraveldocs.payment.common.metrics;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized payment metrics for monitoring payment operations across all providers.
 * Uses Micrometer for integration with Prometheus and other monitoring systems.
 */
@Slf4j
@Component
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> paymentCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> webhookCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> operationTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> deadLetterCounts = new ConcurrentHashMap<>();

    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record a payment creation event
     *
     * @param provider Payment provider (STRIPE, PAYSTACK, etc.)
     * @param type     Payment type (ONE_TIME, SUBSCRIPTION)
     * @param status   Payment status (SUCCEEDED, FAILED, etc.)
     */
    public void recordPaymentCreated(PaymentGateway provider, String type, String status) {
        String key = String.format("payment.created.%s.%s.%s", provider.name(), type, status);
        Counter counter = paymentCounters.computeIfAbsent(key, k ->
                Counter.builder("payment.created")
                        .tag("provider", provider.name())
                        .tag("type", type)
                        .tag("status", status)
                        .description("Number of payments created")
                        .register(meterRegistry)
        );
        counter.increment();
        log.debug("Recorded payment created: provider={}, type={}, status={}", provider, type, status);
    }

    /**
     * Record a successful payment
     */
    public void recordPaymentSuccess(PaymentGateway provider, String type) {
        recordPaymentCreated(provider, type, "SUCCESS");
    }

    /**
     * Record a failed payment
     */
    public void recordPaymentFailure(PaymentGateway provider, String type, String reason) {
        String key = String.format("payment.failed.%s.%s.%s", provider.name(), type, reason);
        Counter counter = paymentCounters.computeIfAbsent(key, k ->
                Counter.builder("payment.failed")
                        .tag("provider", provider.name())
                        .tag("type", type)
                        .tag("reason", reason)
                        .description("Number of failed payments")
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * Record a webhook event processed
     *
     * @param provider  Payment provider
     * @param eventType Webhook event type
     * @param success   Whether processing was successful
     */
    public void recordWebhookProcessed(PaymentGateway provider, String eventType, boolean success) {
        String status = success ? "success" : "failure";
        String key = String.format("webhook.processed.%s.%s.%s", provider.name(), eventType, status);
        Counter counter = webhookCounters.computeIfAbsent(key, k ->
                Counter.builder("payment.webhook.processed")
                        .tag("provider", provider.name())
                        .tag("event_type", eventType)
                        .tag("status", status)
                        .description("Number of webhook events processed")
                        .register(meterRegistry)
        );
        counter.increment();
        log.debug("Recorded webhook processed: provider={}, eventType={}, success={}", provider, eventType, success);
    }

    /**
     * Record a refund event
     */
    public void recordRefund(PaymentGateway provider, String status) {
        String key = String.format("payment.refund.%s.%s", provider.name(), status);
        Counter counter = paymentCounters.computeIfAbsent(key, k ->
                Counter.builder("payment.refund")
                        .tag("provider", provider.name())
                        .tag("status", status)
                        .description("Number of refunds")
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * Record subscription lifecycle event
     */
    public void recordSubscriptionEvent(PaymentGateway provider, String event) {
        String key = String.format("subscription.event.%s.%s", provider.name(), event);
        Counter counter = paymentCounters.computeIfAbsent(key, k ->
                Counter.builder("payment.subscription.event")
                        .tag("provider", provider.name())
                        .tag("event", event)
                        .description("Number of subscription events")
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * Record API call latency
     *
     * @param provider  Payment provider
     * @param operation Operation name (e.g., "create_payment_intent")
     * @param durationMs Duration in milliseconds
     */
    public void recordApiLatency(PaymentGateway provider, String operation, long durationMs) {
        String key = String.format("api.latency.%s.%s", provider.name(), operation);
        Timer timer = operationTimers.computeIfAbsent(key, k ->
                Timer.builder("payment.api.latency")
                        .tag("provider", provider.name())
                        .tag("operation", operation)
                        .description("Payment API call latency")
                        .register(meterRegistry)
        );
        timer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Helper to time an operation
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop timer and record the duration
     */
    public void stopTimer(Timer.Sample sample, PaymentGateway provider, String operation) {
        String key = String.format("api.latency.%s.%s", provider.name(), operation);
        Timer timer = operationTimers.computeIfAbsent(key, k ->
                Timer.builder("payment.api.latency")
                        .tag("provider", provider.name())
                        .tag("operation", operation)
                        .description("Payment API call latency")
                        .register(meterRegistry)
        );
        sample.stop(timer);
    }

    /**
     * Record dead letter queue size
     */
    public void recordDeadLetterCount(PaymentGateway provider, long count) {
        String key = String.format("webhook.dead_letter.%s", provider.name());
        AtomicLong atomicLong = deadLetterCounts.computeIfAbsent(key, k -> {
            AtomicLong counter = new AtomicLong(count);
            meterRegistry.gauge("payment.webhook.dead_letter",
                    io.micrometer.core.instrument.Tags.of("provider", provider.name()),
                    counter,
                    AtomicLong::get);
            return counter;
        });
        atomicLong.set(count);
    }
}
