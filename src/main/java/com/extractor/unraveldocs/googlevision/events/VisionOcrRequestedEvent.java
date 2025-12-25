package com.extractor.unraveldocs.googlevision.events;

import com.extractor.unraveldocs.ocrprocessing.provider.OcrProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event for requesting OCR processing via Google Cloud Vision.
 * Published to Kafka for async processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionOcrRequestedEvent {

    /**
     * Unique identifier for the document to process.
     */
    private String documentId;

    /**
     * Collection ID containing the document.
     */
    private String collectionId;

    /**
     * URL of the image to process.
     */
    private String imageUrl;

    /**
     * MIME type of the file.
     */
    private String mimeType;

    /**
     * User ID who initiated the request.
     */
    private String userId;

    /**
     * The OCR provider to use.
     * Defaults to Google Vision for this event type.
     */
    @Builder.Default
    private OcrProviderType providerType = OcrProviderType.GOOGLE_VISION;

    /**
     * Language hint for OCR (ISO 639-1 code).
     */
    private String languageHint;

    /**
     * Priority for processing (higher = more urgent).
     */
    @Builder.Default
    private int priority = 0;

    /**
     * Whether to enable fallback to Tesseract on failure.
     */
    @Builder.Default
    private boolean fallbackEnabled = true;

    /**
     * Timestamp when the event was created.
     */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Correlation ID for tracing.
     */
    private String correlationId;

    /**
     * Additional metadata for processing.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Create event from basic parameters.
     */
    public static VisionOcrRequestedEvent of(String documentId, String collectionId, String imageUrl, String userId) {
        return VisionOcrRequestedEvent.builder()
                .documentId(documentId)
                .collectionId(collectionId)
                .imageUrl(imageUrl)
                .userId(userId)
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }
}
