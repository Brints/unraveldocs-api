package com.extractor.unraveldocs.ocrprocessing.metrics;

import com.extractor.unraveldocs.ocrprocessing.provider.OcrProviderType;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrResult;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer metrics for OCR processing.
 * Tracks requests, duration, errors, and provider-specific metrics.
 */
@Slf4j
@Component
public class OcrMetrics {

    private static final String METRIC_PREFIX = "ocr";

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<OcrProviderType, Counter> requestCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<OcrProviderType, Counter> successCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<OcrProviderType, Counter> errorCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<OcrProviderType, Timer> durationTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<OcrProviderType, DistributionSummary> confidenceHistograms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<OcrProviderType, DistributionSummary> characterCountHistograms = new ConcurrentHashMap<>();

    public OcrMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize metrics for all provider types
        for (OcrProviderType type : OcrProviderType.values()) {
            initializeMetricsForProvider(type);
        }

        log.info("OcrMetrics initialized");
    }

    private void initializeMetricsForProvider(OcrProviderType type) {
        String providerTag = type.getCode();

        requestCounters.put(type, Counter.builder(METRIC_PREFIX + ".requests.total")
                .description("Total OCR requests")
                .tag("provider", providerTag)
                .register(meterRegistry));

        successCounters.put(type, Counter.builder(METRIC_PREFIX + ".requests.success")
                .description("Successful OCR requests")
                .tag("provider", providerTag)
                .register(meterRegistry));

        errorCounters.put(type, Counter.builder(METRIC_PREFIX + ".requests.errors")
                .description("Failed OCR requests")
                .tag("provider", providerTag)
                .register(meterRegistry));

        durationTimers.put(type, Timer.builder(METRIC_PREFIX + ".requests.duration")
                .description("OCR processing duration")
                .tag("provider", providerTag)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));

        confidenceHistograms.put(type, DistributionSummary.builder(METRIC_PREFIX + ".confidence")
                .description("OCR confidence scores")
                .tag("provider", providerTag)
                .publishPercentiles(0.5, 0.95)
                .scale(100) // Convert to percentage
                .register(meterRegistry));

        characterCountHistograms.put(type, DistributionSummary.builder(METRIC_PREFIX + ".characters.extracted")
                .description("Characters extracted per OCR request")
                .tag("provider", providerTag)
                .publishPercentiles(0.5, 0.95)
                .register(meterRegistry));
    }

    /**
     * Record the start of an OCR request.
     */
    public void recordRequestStart(OcrProviderType provider) {
        requestCounters.get(provider).increment();
    }

    /**
     * Record a successful OCR result.
     */
    public void recordSuccess(OcrResult result) {
        OcrProviderType provider = result.getProviderType();

        successCounters.get(provider).increment();
        durationTimers.get(provider).record(result.getProcessingTimeMs(), TimeUnit.MILLISECONDS);
        characterCountHistograms.get(provider).record(result.getCharacterCount());

        if (result.getConfidence() != null) {
            confidenceHistograms.get(provider).record(result.getConfidence());
        }
    }

    /**
     * Record a failed OCR request.
     */
    public void recordError(OcrProviderType provider, long processingTimeMs, String errorType) {
        errorCounters.get(provider).increment();
        durationTimers.get(provider).record(processingTimeMs, TimeUnit.MILLISECONDS);

        // Record error type
        meterRegistry.counter(
                METRIC_PREFIX + ".errors.by.type",
                "provider", provider.getCode(),
                "error_type", sanitizeErrorType(errorType)).increment();
    }

    /**
     * Record a fallback from primary to secondary provider.
     */
    public void recordFallback(OcrProviderType primary, OcrProviderType fallback) {
        meterRegistry.counter(
                METRIC_PREFIX + ".fallbacks.total",
                "primary", primary.getCode(),
                "fallback", fallback.getCode()).increment();
    }

    /**
     * Record quota usage for a user.
     */
    public void recordQuotaUsage(String userId, String tier, OcrProviderType provider) {
        meterRegistry.counter(
                METRIC_PREFIX + ".quota.usage",
                "tier", tier,
                "provider", provider.getCode()).increment();
    }

    /**
     * Record quota exceeded event.
     */
    public void recordQuotaExceeded(String userId, String tier) {
        meterRegistry.counter(
                METRIC_PREFIX + ".quota.exceeded",
                "tier", tier).increment();
    }

    /**
     * Create a timer sample for measuring duration.
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop a timer and record to the appropriate provider timer.
     */
    public void stopTimer(Timer.Sample sample, OcrProviderType provider) {
        sample.stop(durationTimers.get(provider));
    }

    /**
     * Sanitize error type for use as a metric tag.
     */
    private String sanitizeErrorType(String errorType) {
        if (errorType == null) {
            return "unknown";
        }
        return errorType
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .substring(0, Math.min(errorType.length(), 50));
    }
}
