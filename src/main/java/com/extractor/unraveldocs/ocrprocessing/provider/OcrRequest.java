package com.extractor.unraveldocs.ocrprocessing.provider;

import com.extractor.unraveldocs.ocrprocessing.dto.request.PdfPageRange;
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
     * Start page for PDF extraction (1-indexed, inclusive). Null = start from first
     * page.
     */
    private Integer startPage;

    /**
     * End page for PDF extraction (1-indexed, inclusive). Null = process to last
     * page.
     */
    private Integer endPage;

    /**
     * Specific pages for PDF extraction (1-indexed). Null = use range or all pages.
     * Takes priority over startPage/endPage when set.
     */
    private java.util.List<Integer> pages;

    /**
     * Build a PdfPageRange from the page selection fields.
     * Returns null if no page selection is specified.
     */
    public PdfPageRange getPdfPageRange() {
        if (pages != null && !pages.isEmpty()) {
            return new PdfPageRange(pages);
        }
        if (startPage == null && endPage == null) {
            return null;
        }
        return new PdfPageRange(startPage, endPage);
    }

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
}
