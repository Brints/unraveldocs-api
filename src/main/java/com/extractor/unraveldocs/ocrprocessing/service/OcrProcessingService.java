package com.extractor.unraveldocs.ocrprocessing.service;

import com.extractor.unraveldocs.ocrprocessing.config.OcrProperties;
import com.extractor.unraveldocs.ocrprocessing.exception.OcrProcessingException;
import com.extractor.unraveldocs.ocrprocessing.metrics.OcrMetrics;
import com.extractor.unraveldocs.ocrprocessing.provider.*;
import com.extractor.unraveldocs.ocrprocessing.quota.OcrQuotaService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * High-level OCR processing service.
 * Orchestrates provider selection, quota checking, fallback logic, and metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrProcessingService {

    private final OcrProviderFactory providerFactory;
    private final OcrQuotaService quotaService;
    private final OcrMetrics ocrMetrics;
    private final OcrProperties ocrProperties;

    /**
     * Process an OCR request with automatic provider selection and fallback.
     *
     * @param request  The OCR request
     * @param userId   The user ID for quota tracking
     * @param userTier The user's subscription tier
     * @return The OCR result
     * @throws OcrProcessingException if processing fails and fallback is not
     *                                available
     */
    public OcrResult processOcr(OcrRequest request, String userId, String userTier) {
        // Check quota
        if (!quotaService.hasRemainingQuota(userId, userTier)) {
            throw OcrProcessingException.quotaExceeded(userId, ocrProperties.getDefaultProvider());
        }

        Timer.Sample timerSample = ocrMetrics.startTimer();
        OcrProvider primaryProvider = null;

        try {
            // Get provider based on request preferences
            primaryProvider = providerFactory.getProviderForRequest(request);
            ocrMetrics.recordRequestStart(primaryProvider.getProviderType());

            log.info("Processing OCR for document {} using provider: {}",
                    request.getDocumentId(), primaryProvider.getProviderType());

            // Process with primary provider
            OcrResult result = primaryProvider.extractText(request);

            if (result.isSuccess()) {
                // Consume quota on success
                quotaService.consumeQuota(userId, userTier, primaryProvider.getProviderType());
                ocrMetrics.recordSuccess(result);
                return result;
            }

            // Primary failed, try fallback if enabled
            return handleFailureWithFallback(request, primaryProvider, result, userId, userTier);

        } catch (OcrProcessingException e) {
            // Non-retryable exception or specific error
            if (!e.isRetryable() || !shouldTryFallback(request)) {
                throw e;
            }

            // Try fallback
            return handleExceptionWithFallback(request, primaryProvider, e, userId, userTier);

        } finally {
            if (primaryProvider != null) {
                ocrMetrics.stopTimer(timerSample, primaryProvider.getProviderType());
            }
        }
    }

    /**
     * Process an OCR request with a specific provider (no fallback).
     *
     * @param request      The OCR request
     * @param providerType The specific provider to use
     * @param userId       The user ID for quota tracking
     * @param userTier     The user's subscription tier
     * @return The OCR result
     */
    public OcrResult processOcrWithProvider(
            OcrRequest request,
            OcrProviderType providerType,
            String userId,
            String userTier) {

        // Check quota
        if (!quotaService.hasRemainingQuota(userId, userTier)) {
            throw OcrProcessingException.quotaExceeded(userId, providerType);
        }

        OcrProvider provider = providerFactory.getProvider(providerType);
        ocrMetrics.recordRequestStart(providerType);

        Timer.Sample timerSample = ocrMetrics.startTimer();

        try {
            OcrResult result = provider.extractText(request);

            if (result.isSuccess()) {
                quotaService.consumeQuota(userId, userTier, providerType);
                ocrMetrics.recordSuccess(result);
            } else {
                ocrMetrics.recordError(providerType, result.getProcessingTimeMs(), result.getErrorMessage());
            }

            return result;

        } finally {
            ocrMetrics.stopTimer(timerSample, providerType);
        }
    }

    /**
     * Handle a failed result by trying the fallback provider.
     */
    private OcrResult handleFailureWithFallback(
            OcrRequest request,
            OcrProvider failedProvider,
            OcrResult failedResult,
            String userId,
            String userTier) {

        ocrMetrics.recordError(
                failedProvider.getProviderType(),
                failedResult.getProcessingTimeMs(),
                failedResult.getErrorMessage());

        if (!shouldTryFallback(request)) {
            return failedResult;
        }

        Optional<OcrProvider> fallbackProvider = getFallbackProvider(failedProvider.getProviderType());

        if (fallbackProvider.isEmpty()) {
            log.warn("No fallback provider available for {}", failedProvider.getProviderType());
            return failedResult;
        }

        return executeFallback(request, failedProvider.getProviderType(), fallbackProvider.get(), userId, userTier);
    }

    /**
     * Handle an exception by trying the fallback provider.
     */
    private OcrResult handleExceptionWithFallback(
            OcrRequest request,
            OcrProvider failedProvider,
            OcrProcessingException exception,
            String userId,
            String userTier) {

        OcrProviderType failedType = failedProvider != null
                ? failedProvider.getProviderType()
                : ocrProperties.getDefaultProvider();

        ocrMetrics.recordError(failedType, 0, exception.getMessage());

        Optional<OcrProvider> fallbackProvider = getFallbackProvider(failedType);

        if (fallbackProvider.isEmpty()) {
            log.warn("No fallback provider available, rethrowing exception");
            throw exception;
        }

        return executeFallback(request, failedType, fallbackProvider.get(), userId, userTier);
    }

    /**
     * Execute OCR with the fallback provider.
     */
    private OcrResult executeFallback(
            OcrRequest request,
            OcrProviderType primaryType,
            OcrProvider fallbackProvider,
            String userId,
            String userTier) {

        OcrProviderType fallbackType = fallbackProvider.getProviderType();

        log.info("Falling back from {} to {} for document {}",
                primaryType, fallbackType, request.getDocumentId());

        ocrMetrics.recordFallback(primaryType, fallbackType);
        ocrMetrics.recordRequestStart(fallbackType);

        Timer.Sample timerSample = ocrMetrics.startTimer();

        try {
            OcrResult result = fallbackProvider.extractText(request);

            if (result.isSuccess()) {
                quotaService.consumeQuota(userId, userTier, fallbackType);
                ocrMetrics.recordSuccess(result);
                result.withMetadata("fallbackFrom", primaryType.getCode());
            } else {
                ocrMetrics.recordError(fallbackType, result.getProcessingTimeMs(), result.getErrorMessage());
            }

            return result;

        } finally {
            ocrMetrics.stopTimer(timerSample, fallbackType);
        }
    }

    /**
     * Check if fallback should be attempted for the request.
     */
    private boolean shouldTryFallback(OcrRequest request) {
        return request.isFallbackEnabled() && ocrProperties.isFallbackEnabled();
    }

    /**
     * Get the fallback provider for a failed provider type.
     */
    private Optional<OcrProvider> getFallbackProvider(OcrProviderType failedType) {
        OcrProviderType fallbackType = ocrProperties.getFallbackProvider();

        // Don't fallback to the same provider
        if (fallbackType == failedType) {
            // Try the other provider
            fallbackType = failedType == OcrProviderType.GOOGLE_VISION
                    ? OcrProviderType.TESSERACT
                    : OcrProviderType.GOOGLE_VISION;
        }

        return providerFactory.getProviderOptional(fallbackType);
    }

    /**
     * Check if a specific provider is available.
     */
    public boolean isProviderAvailable(OcrProviderType providerType) {
        return providerFactory.isProviderAvailable(providerType);
    }

    /**
     * Get remaining quota for a user.
     */
    public long getRemainingQuota(String userId, String userTier) {
        return quotaService.getRemainingQuota(userId, userTier);
    }
}
