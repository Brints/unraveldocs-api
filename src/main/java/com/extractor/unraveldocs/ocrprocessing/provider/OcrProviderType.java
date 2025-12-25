package com.extractor.unraveldocs.ocrprocessing.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing the available OCR provider types.
 * Each provider has a unique code and human-readable display name.
 */
@Getter
@RequiredArgsConstructor
public enum OcrProviderType {

    /**
     * Local Tesseract OCR engine.
     * Pros: Free, offline, no API costs
     * Cons: Lower accuracy for complex documents
     */
    TESSERACT("tesseract", "Local Tesseract OCR"),

    /**
     * Google Cloud Vision API.
     * Pros: High accuracy, supports many languages, handles complex layouts
     * Cons: API costs ($1.50/1000 images), requires internet
     */
    GOOGLE_VISION("google-vision", "Google Cloud Vision API");

    /**
     * Unique identifier code for the provider.
     * Used in configuration and API requests.
     */
    private final String code;

    /**
     * Human-readable display name for the provider.
     */
    private final String displayName;

    /**
     * Find a provider type by its code.
     *
     * @param code The provider code to search for
     * @return The matching OcrProviderType
     * @throws IllegalArgumentException if no provider matches the code
     */
    public static OcrProviderType fromCode(String code) {
        for (OcrProviderType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OCR provider type: " + code);
    }
}
