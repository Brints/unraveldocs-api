package com.extractor.unraveldocs.ocrprocessing.provider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Core interface for OCR provider implementations.
 * Defines the contract for all OCR providers (Tesseract, Google Vision, etc.).
 * 
 * <p>
 * Implementations should be thread-safe and handle their own connection
 * management.
 * </p>
 */
public interface OcrProvider {

    /**
     * Get the provider type identifier.
     *
     * @return The OCR provider type
     */
    OcrProviderType getProviderType();

    /**
     * Extract text from a document synchronously.
     *
     * @param request The OCR request containing image data and options
     * @return The OCR result with extracted text and metadata
     */
    OcrResult extractText(OcrRequest request);

    /**
     * Extract text from a document asynchronously.
     * Default implementation wraps the synchronous method.
     *
     * @param request The OCR request containing image data and options
     * @return A CompletableFuture that completes with the OCR result
     */
    default CompletableFuture<OcrResult> extractTextAsync(OcrRequest request) {
        return CompletableFuture.supplyAsync(() -> extractText(request));
    }

    /**
     * Check if this provider supports the given MIME type.
     *
     * @param mimeType The MIME type to check (e.g., "image/png", "application/pdf")
     * @return true if the provider can process this MIME type
     */
    boolean supports(String mimeType);

    /**
     * Get the list of supported languages.
     *
     * @return List of ISO 639-1 language codes supported by this provider
     */
    List<String> getSupportedLanguages();

    /**
     * Check if the provider is currently available and healthy.
     * Used for health checks and fallback decisions.
     *
     * @return true if the provider is available for processing
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Get the provider's display name for logging and UI.
     *
     * @return Human-readable provider name
     */
    default String getDisplayName() {
        return getProviderType().getDisplayName();
    }

    /**
     * Get the maximum file size this provider can handle in bytes.
     *
     * @return Maximum file size in bytes, or -1 for no limit
     */
    default long getMaxFileSizeBytes() {
        return -1; // No limit by default
    }
}
