package com.extractor.unraveldocs.googlevision.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for Google Cloud Vision API.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ocr.google-vision")
public class GoogleVisionProperties {

    /**
     * Whether Google Cloud Vision is enabled.
     */
    private boolean enabled = false;

    /**
     * Timeout for Vision API calls in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Maximum retries for Vision API calls.
     */
    private int maxRetries = 3;

    /**
     * Delay between retries in milliseconds.
     */
    private long retryDelayMs = 1000;

    /**
     * Supported languages (ISO 639-1 codes).
     */
    private List<String> languages = List.of(
            "en", "es", "fr", "de", "it", "pt", "nl", "pl", "ru",
            "zh", "ja", "ko", "ar", "hi", "th", "vi");

    /**
     * Whether to include confidence scores in results.
     */
    private boolean includeConfidence = true;

    /**
     * Whether to detect and return document language.
     */
    private boolean detectLanguage = true;

    /**
     * Maximum image size in megapixels.
     * Vision API has a limit of 75 megapixels.
     */
    private int maxImageMegapixels = 75;

    /**
     * Maximum file size in bytes.
     * Default: 20MB (Vision API limit)
     */
    private long maxFileSizeBytes = 20 * 1024 * 1024;

    /**
     * Path to Google Cloud credentials JSON file.
     * Supports:
     * - classpath: prefix for classpath resources (e.g.,
     * classpath:google-credentials.json)
     * - file: prefix for file system paths (e.g., file:/path/to/credentials.json)
     * - Absolute paths (e.g., /path/to/credentials.json or
     * C:\path\to\credentials.json)
     * 
     * If not set, falls back to GOOGLE_APPLICATION_CREDENTIALS environment variable
     * or Application Default Credentials (ADC).
     */
    private String credentialsLocation;

    /**
     * Feature types to use for text detection.
     */
    private FeatureConfig features = new FeatureConfig();

    /**
     * Feature configuration for Vision API.
     */
    @Data
    public static class FeatureConfig {
        /**
         * Use TEXT_DETECTION for simple images.
         */
        private boolean textDetection = true;

        /**
         * Use DOCUMENT_TEXT_DETECTION for dense text/documents.
         */
        private boolean documentTextDetection = true;

        /**
         * Maximum results per feature.
         */
        private int maxResults = 50;
    }
}
