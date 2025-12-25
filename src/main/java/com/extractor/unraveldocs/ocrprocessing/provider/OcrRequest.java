package com.extractor.unraveldocs.ocrprocessing.provider;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Standardized request DTO for OCR processing.
 * Contains all information needed to perform OCR on a document.
 */
@Data
@Builder
public class OcrRequest {

    /**
     * URL of the image to process.
     * Either imageUrl or imageBytes must be provided.
     */
    private String imageUrl;

    /**
     * Raw bytes of the image (alternative to URL).
     * Either imageUrl or imageBytes must be provided.
     */
    private byte[] imageBytes;

    /**
     * MIME type of the file (e.g., "image/png", "image/jpeg", "application/pdf").
     */
    private String mimeType;

    /**
     * Language hint for OCR processing.
     * ISO 639-1 language code (e.g., "en", "de", "es").
     * If null, providers will attempt auto-detection.
     */
    private String language;

    /**
     * Reference to the document entity ID in the database.
     */
    private String documentId;

    /**
     * Reference to the collection entity ID.
     */
    private String collectionId;

    /**
     * User ID who initiated the OCR request.
     */
    private String userId;

    /**
     * Additional metadata for processing.
     * Can include provider-specific settings.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Priority for processing (higher = more urgent).
     * Used for queue ordering.
     */
    @Builder.Default
    private int priority = 0;

    /**
     * Preferred OCR provider type.
     * If null, the default configured provider will be used.
     */
    private OcrProviderType preferredProvider;

    /**
     * Whether to enable fallback to another provider on failure.
     */
    @Builder.Default
    private boolean fallbackEnabled = true;

    /**
     * Check if the request has image data via URL.
     */
    public boolean hasImageUrl() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    /**
     * Check if the request has image data via bytes.
     */
    public boolean hasImageBytes() {
        return imageBytes != null && imageBytes.length > 0;
    }

    /**
     * Check if the request is valid (has at least one source of image data).
     */
    public boolean isValid() {
        return hasImageUrl() || hasImageBytes();
    }

    /**
     * Add a metadata entry.
     */
    public OcrRequest withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
}
