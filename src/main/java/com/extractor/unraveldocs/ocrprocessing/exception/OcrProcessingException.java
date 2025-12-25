package com.extractor.unraveldocs.ocrprocessing.exception;

import com.extractor.unraveldocs.ocrprocessing.provider.OcrProviderType;
import lombok.Getter;

/**
 * Exception thrown when OCR processing fails.
 * Contains information about the provider and failure context.
 */
@Getter
public class OcrProcessingException extends RuntimeException {

    private final OcrProviderType providerType;
    private final String documentId;
    private final boolean retryable;

    public OcrProcessingException(String message, OcrProviderType providerType) {
        super(message);
        this.providerType = providerType;
        this.documentId = null;
        this.retryable = true;
    }

    public OcrProcessingException(String message, OcrProviderType providerType, Throwable cause) {
        super(message, cause);
        this.providerType = providerType;
        this.documentId = null;
        this.retryable = true;
    }

    public OcrProcessingException(String message, OcrProviderType providerType, String documentId, boolean retryable) {
        super(message);
        this.providerType = providerType;
        this.documentId = documentId;
        this.retryable = retryable;
    }

    public OcrProcessingException(String message, OcrProviderType providerType, String documentId, boolean retryable,
            Throwable cause) {
        super(message, cause);
        this.providerType = providerType;
        this.documentId = documentId;
        this.retryable = retryable;
    }

    /**
     * Create an exception for provider unavailability.
     */
    public static OcrProcessingException providerUnavailable(OcrProviderType providerType) {
        return new OcrProcessingException(
                "OCR provider is not available: " + providerType.getDisplayName(),
                providerType,
                null,
                true);
    }

    /**
     * Create an exception for unsupported file type.
     */
    public static OcrProcessingException unsupportedFileType(String mimeType, OcrProviderType providerType) {
        return new OcrProcessingException(
                "Unsupported file type for OCR: " + mimeType,
                providerType,
                null,
                false);
    }

    /**
     * Create an exception for quota exceeded.
     */
    public static OcrProcessingException quotaExceeded(String userId, OcrProviderType providerType) {
        return new OcrProcessingException(
                "OCR quota exceeded for user: " + userId,
                providerType,
                null,
                false);
    }

    /**
     * Create an exception for timeout.
     */
    public static OcrProcessingException timeout(OcrProviderType providerType, String documentId) {
        return new OcrProcessingException(
                "OCR processing timed out",
                providerType,
                documentId,
                true);
    }
}
