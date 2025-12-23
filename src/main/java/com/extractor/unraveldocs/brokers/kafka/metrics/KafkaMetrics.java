package com.extractor.unraveldocs.brokers.kafka.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kafka messaging metrics for monitoring and observability.
 * Tracks message send/receive rates, latencies, and error counts.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaMetrics {

    private final MeterRegistry meterRegistry;

    // Counters for message counts
    private final ConcurrentMap<String, Counter> messagesSentCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> messagesReceivedCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> messagesFailedCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> messagesDlqCounters = new ConcurrentHashMap<>();

    // Timers for latency tracking
    private final ConcurrentMap<String, Timer> sendLatencyTimers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> processLatencyTimers = new ConcurrentHashMap<>();

    private static final String METRIC_PREFIX = "kafka.messaging";

    public KafkaMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("Kafka metrics initialized");
    }

    /**
     * Record a successfully sent message.
     *
     * @param topic the topic the message was sent to
     */
    public void recordMessageSent(String topic) {
        getOrCreateCounter(messagesSentCounters, topic, "messages.sent", "sent").increment();
    }

    /**
     * Record a successfully received message.
     *
     * @param topic the topic the message was received from
     */
    public void recordMessageReceived(String topic) {
        getOrCreateCounter(messagesReceivedCounters, topic, "messages.received", "received").increment();
    }

    /**
     * Record a failed message send attempt.
     *
     * @param topic the topic the message failed to send to
     */
    public void recordMessageFailed(String topic) {
        getOrCreateCounter(messagesFailedCounters, topic, "messages.failed", "failed").increment();
    }

    /**
     * Record a message sent to DLQ.
     *
     * @param topic the original topic (not the DLQ topic)
     */
    public void recordMessageToDlq(String topic) {
        getOrCreateCounter(messagesDlqCounters, topic, "messages.dlq", "dlq").increment();
    }

    /**
     * Record send latency for a message.
     *
     * @param topic the topic the message was sent to
     * @param latency the time taken to send the message
     */
    public void recordSendLatency(String topic, Duration latency) {
        getOrCreateTimer(sendLatencyTimers, topic, "send.latency", "send")
                .record(latency);
    }

    /**
     * Record processing latency for a received message.
     *
     * @param topic the topic the message was received from
     * @param latency the time taken to process the message
     */
    public void recordProcessLatency(String topic, Duration latency) {
        getOrCreateTimer(processLatencyTimers, topic, "process.latency", "process")
                .record(latency);
    }

    /**
     * Start a timer for measuring send latency.
     *
     * @return a Timer.Sample that can be used to record the latency
     */
    public Timer.Sample startSendTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop a timer and record the send latency.
     *
     * @param sample the Timer.Sample that was started
     * @param topic the topic the message was sent to
     */
    public void stopSendTimer(Timer.Sample sample, String topic) {
        sample.stop(getOrCreateTimer(sendLatencyTimers, topic, "send.latency", "send"));
    }

    /**
     * Start a timer for measuring process latency.
     *
     * @return a Timer.Sample that can be used to record the latency
     */
    public Timer.Sample startProcessTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop a timer and record the process latency.
     *
     * @param sample the Timer.Sample that was started
     * @param topic the topic the message was received from
     */
    public void stopProcessTimer(Timer.Sample sample, String topic) {
        sample.stop(getOrCreateTimer(processLatencyTimers, topic, "process.latency", "process"));
    }

    private Counter getOrCreateCounter(ConcurrentMap<String, Counter> cache,
                                        String topic,
                                        String metricName,
                                        String description) {
        return cache.computeIfAbsent(topic, t ->
                Counter.builder(METRIC_PREFIX + "." + metricName)
                        .tag("topic", topic)
                        .description("Number of messages " + description + " for topic " + topic)
                        .register(meterRegistry)
        );
    }

    private Timer getOrCreateTimer(ConcurrentMap<String, Timer> cache,
                                    String topic,
                                    String metricName,
                                    String operation) {
        return cache.computeIfAbsent(topic, t ->
                Timer.builder(METRIC_PREFIX + "." + metricName)
                        .tag("topic", topic)
                        .description("Latency of " + operation + " operations for topic " + topic)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .register(meterRegistry)
        );
    }
}

