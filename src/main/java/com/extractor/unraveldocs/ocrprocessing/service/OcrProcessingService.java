package com.extractor.unraveldocs.ocrprocessing.service;

import com.extractor.unraveldocs.credit.service.CreditBalanceService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.ocrprocessing.config.OcrProperties;
import com.extractor.unraveldocs.ocrprocessing.exception.OcrProcessingException;
import com.extractor.unraveldocs.ocrprocessing.metrics.OcrMetrics;
import com.extractor.unraveldocs.ocrprocessing.provider.*;
import com.extractor.unraveldocs.ocrprocessing.quota.OcrQuotaService;
import com.extractor.unraveldocs.storage.service.StorageAllocationService;
import com.extractor.unraveldocs.subscription.service.SubscriptionFeatureService;
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
    private final StorageAllocationService storageAllocationService;
    private final OcrMetrics ocrMetrics;
    private final OcrProperties ocrProperties;
    private final SanitizeLogging sanitizer;
    private final CreditBalanceService creditBalanceService;
    private final SubscriptionFeatureService subscriptionFeatureService;

    /**
     * Process an OCR request with automatic provider selection based on
     * subscription plan and credit balance.
     *
     * Provider selection rules:
     * - Paid plan (any tier) → Google Vision (no credits deducted)
     * - Free plan + enough credits → Google Vision (credits deducted)
     * - Free plan + not enough credits → Tesseract (no credits deducted)
     *
     * @param request The OCR request
     * @param userId  The user ID for quota tracking and provider resolution
     * @return The OCR result
     * @throws OcrProcessingException if processing fails and fallback is not
     *                                available
     */
    public OcrResult processOcr(OcrRequest request, String userId) {
        Timer.Sample timerSample = ocrMetrics.startTimer();
        OcrProvider primaryProvider = null;

        try {
            // Resolve provider based on subscription + credit balance
            OcrProviderType providerType = resolveProvider(userId);
            primaryProvider = getProviderWithFallbackToDefault(providerType);
            ocrMetrics.recordRequestStart(primaryProvider.getProviderType());

            boolean isPaid = subscriptionFeatureService.hasPaidSubscription(userId);
            log.info("Processing OCR for document {} using provider: {} (paid: {})",
                    sanitizer.sanitizeLogging(request.getDocumentId()),
                    sanitizer.sanitizeLoggingObject(primaryProvider.getProviderType()),
                    isPaid);

            // Process with primary provider
            OcrResult result = primaryProvider.extractText(request);

            if (result.isSuccess()) {
                storageAllocationService.updateOcrUsage(userId, 1);
                ocrMetrics.recordSuccess(result);
                return result;
            }

            // Primary failed, try fallback if enabled
            ocrMetrics.stopTimer(timerSample, primaryProvider.getProviderType());
            timerSample = null; // Reset timer sample for fallback timing
            return handleFailureWithFallback(request, primaryProvider, result, userId);

        } catch (OcrProcessingException e) {
            // Non-retryable exception or specific error
            if (!e.isRetryable() || !shouldTryFallback(request)) {
                throw e;
            }

            // Try fallback
            if (primaryProvider != null) {
                assert timerSample != null;
                ocrMetrics.stopTimer(timerSample, primaryProvider.getProviderType());
                timerSample = null; // Reset timer sample for fallback timing
            }
            return handleExceptionWithFallback(request, primaryProvider, e, userId);

        } finally {
            if (primaryProvider != null && timerSample != null) {
                ocrMetrics.stopTimer(timerSample, primaryProvider.getProviderType());
            }
        }
    }

    /**
     * Resolve the OCR provider based on subscription plan and credit balance.
     *
     * - Paid plan → Google Vision (no credits deducted)
     * - Free plan + enough credits → Google Vision (credits deducted after OCR)
     * - Free plan + not enough credits → Tesseract (no credits deducted)
     *
     * @param userId The user's ID
     * @return The appropriate OCR provider type
     */
    OcrProviderType resolveProvider(String userId) {
        boolean isPaid = subscriptionFeatureService.hasPaidSubscription(userId);

        if (isPaid) {
            log.debug("User {} has paid subscription, using Google Vision OCR",
                    sanitizer.sanitizeLogging(userId));
            return OcrProviderType.GOOGLE_VISION;
        }

        // Free plan: check credit balance
        boolean hasCredits = creditBalanceService.hasEnoughCredits(userId, 1);
        if (hasCredits) {
            log.debug("Free user {} has credits, using Google Vision OCR",
                    sanitizer.sanitizeLogging(userId));
            return OcrProviderType.GOOGLE_VISION;
        }

        log.debug("Free user {} has no credits, using Tesseract OCR",
                sanitizer.sanitizeLogging(userId));
        return OcrProviderType.TESSERACT;
    }

    /**
     * Determine if credits should be deducted for this OCR operation.
     * Credits are only deducted for free plan users when Google Vision is used.
     *
     * @param userId       The user's ID
     * @param providerType The provider that was actually used
     * @return true if credits should be deducted
     */
    public boolean shouldDeductCredits(String userId, OcrProviderType providerType) {
        if (providerType != OcrProviderType.GOOGLE_VISION) {
            return false;
        }
        return !subscriptionFeatureService.hasPaidSubscription(userId);
    }

    /**
     * Get provider for the specified type, falling back to default if unavailable.
     *
     * @param preferredType The preferred provider type
     * @return An available provider
     */
    private OcrProvider getProviderWithFallbackToDefault(OcrProviderType preferredType) {
        // Try to get the preferred provider
        if (providerFactory.isProviderAvailable(preferredType)) {
            return providerFactory.getProvider(preferredType);
        }

        // Preferred not available, log and fall back to default
        log.warn("Preferred OCR provider {} is not available, falling back to default: {}",
                sanitizer.sanitizeLoggingObject(preferredType),
                sanitizer.sanitizeLoggingObject(ocrProperties.getDefaultProvider()));

        return providerFactory.getDefaultProvider();
    }

    /**
     * Process an OCR request with a specific provider (no fallback).
     *
     * @param request      The OCR request
     * @param providerType The specific provider to use
     * @param userId       The user ID for quota tracking
     * @return The OCR result
     */
    public OcrResult processOcrWithProvider(
            OcrRequest request,
            OcrProviderType providerType,
            String userId) {

        OcrProvider provider = providerFactory.getProvider(providerType);
        ocrMetrics.recordRequestStart(providerType);

        Timer.Sample timerSample = ocrMetrics.startTimer();

        try {
            OcrResult result = provider.extractText(request);

            if (result.isSuccess()) {
                storageAllocationService.updateOcrUsage(userId, 1);
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
            String userId) {

        ocrMetrics.recordError(
                failedProvider.getProviderType(),
                failedResult.getProcessingTimeMs(),
                failedResult.getErrorMessage());

        if (!shouldTryFallback(request)) {
            return failedResult;
        }

        Optional<OcrProvider> fallbackProvider = getFallbackProvider(failedProvider.getProviderType());

        if (fallbackProvider.isEmpty()) {
            log.warn("No fallback provider available for {}",
                    sanitizer.sanitizeLoggingObject(failedProvider.getProviderType()));
            return failedResult;
        }

        return executeFallback(request, failedProvider.getProviderType(), fallbackProvider.get(), userId);
    }

    /**
     * Handle an exception by trying the fallback provider.
     */
    private OcrResult handleExceptionWithFallback(
            OcrRequest request,
            OcrProvider failedProvider,
            OcrProcessingException exception,
            String userId) {

        OcrProviderType failedType = failedProvider != null
                ? failedProvider.getProviderType()
                : ocrProperties.getDefaultProvider();

        ocrMetrics.recordError(failedType, 0, exception.getMessage());

        Optional<OcrProvider> fallbackProvider = getFallbackProvider(failedType);

        if (fallbackProvider.isEmpty()) {
            log.warn("No fallback provider available, rethrowing exception");
            throw exception;
        }

        return executeFallback(request, failedType, fallbackProvider.get(), userId);
    }

    /**
     * Execute OCR with the fallback provider.
     */
    private OcrResult executeFallback(
            OcrRequest request,
            OcrProviderType primaryType,
            OcrProvider fallbackProvider,
            String userId) {

        OcrProviderType fallbackType = fallbackProvider.getProviderType();

        log.info("Falling back from {} to {} for document {}",
                sanitizer.sanitizeLoggingObject(primaryType),
                sanitizer.sanitizeLoggingObject(fallbackType),
                sanitizer.sanitizeLogging(request.getDocumentId()));

        ocrMetrics.recordFallback(primaryType, fallbackType);
        ocrMetrics.recordRequestStart(fallbackType);

        Timer.Sample timerSample = ocrMetrics.startTimer();

        try {
            OcrResult result = fallbackProvider.extractText(request);

            if (result.isSuccess()) {
                storageAllocationService.updateOcrUsage(userId, 1);
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
}
