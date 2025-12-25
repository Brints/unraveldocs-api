package com.extractor.unraveldocs.ocrprocessing.provider;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized result DTO for OCR processing.
 * Contains the extracted text and processing metadata.
 */
@Data
@Builder
public class OcrResult {

    /**
     * The extracted text content from the document.
     */
    private String extractedText;

    /**
     * Confidence score from 0.0 to 1.0.
     * Higher scores indicate higher confidence in accuracy.
     * May be null if the provider doesn't support confidence scores.
     */
    private Double confidence;

    /**
     * The provider type that processed this request.
     */
    private OcrProviderType providerType;

    /**
     * Time taken for processing in milliseconds.
     */
    private long processingTimeMs;

    /**
     * Detected language of the text (ISO 639-1 code).
     * May be null if language detection is not supported or failed.
     */
    private String languageDetected;

    /**
     * Error message if processing failed.
     */
    private String errorMessage;

    /**
     * Whether the OCR processing was successful.
     */
    private boolean success;

    /**
     * Reference to the original document ID.
     */
    private String documentId;

    /**
     * Timestamp when processing was completed.
     */
    @Builder.Default
    private Instant processedAt = Instant.now();

    /**
     * Additional metadata from the OCR provider.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Number of characters extracted.
     */
    public int getCharacterCount() {
        return extractedText != null ? extractedText.length() : 0;
    }

    /**
     * Number of words extracted (approximate).
     */
    public int getWordCount() {
        if (extractedText == null || extractedText.isBlank()) {
            return 0;
        }
        return extractedText.split("\\s+").length;
    }

    /**
     * Create a successful result.
     */
    public static OcrResult success(String extractedText, OcrProviderType provider, long processingTimeMs) {
        return OcrResult.builder()
                .extractedText(extractedText)
                .providerType(provider)
                .processingTimeMs(processingTimeMs)
                .success(true)
                .build();
    }

    /**
     * Create a failed result.
     */
    public static OcrResult failure(String errorMessage, OcrProviderType provider, long processingTimeMs) {
        return OcrResult.builder()
                .errorMessage(errorMessage)
                .providerType(provider)
                .processingTimeMs(processingTimeMs)
                .success(false)
                .build();
    }

    /**
     * Create a failed result with an exception.
     */
    public static OcrResult failure(Exception e, OcrProviderType provider, long processingTimeMs) {
        return failure(e.getMessage(), provider, processingTimeMs);
    }

    /**
     * Add metadata to the result.
     */
    public OcrResult withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
}
