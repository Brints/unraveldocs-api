package com.extractor.unraveldocs.ocrprocessing.config;

import com.extractor.unraveldocs.ocrprocessing.provider.OcrProviderType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for OCR processing.
 * Allows externalized configuration of provider selection, fallback behavior,
 * and quotas.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {

    /**
     * The default OCR provider to use.
     * Can be overridden per request.
     */
    private OcrProviderType defaultProvider = OcrProviderType.TESSERACT;

    /**
     * Whether to enable fallback to another provider if the primary fails.
     */
    private boolean fallbackEnabled = true;

    /**
     * The provider to use as fallback when the primary provider fails.
     */
    private OcrProviderType fallbackProvider = OcrProviderType.TESSERACT;

    /**
     * List of enabled providers.
     * Providers not in this list will not be available for use.
     */
    private List<OcrProviderType> enabledProviders = new ArrayList<>(List.of(OcrProviderType.TESSERACT));

    /**
     * Maximum file size allowed for OCR processing in bytes.
     * Default: 10MB
     */
    private long maxFileSizeBytes = 10 * 1024 * 1024;

    /**
     * Timeout for OCR processing in seconds.
     */
    private int timeoutSeconds = 60;

    /**
     * Maximum number of retries for failed OCR requests.
     */
    private int maxRetries = 3;

    /**
     * Quota configuration for rate limiting and usage tracking.
     */
    private QuotaConfig quota = new QuotaConfig();

    /**
     * Google Cloud Vision specific settings.
     */
    private GoogleVisionConfig googleVision = new GoogleVisionConfig();

    /**
     * Tesseract specific settings.
     */
    private TesseractConfig tesseract = new TesseractConfig();

    /**
     * Quota configuration for controlling OCR usage.
     */
    @Data
    public static class QuotaConfig {
        /**
         * Whether to enable quota enforcement.
         */
        private boolean enabled = true;

        /**
         * Default daily limit per user for free tier.
         */
        private int freeTierDailyLimit = 50;

        /**
         * Daily limit for basic tier users.
         */
        private int basicTierDailyLimit = 200;

        /**
         * Daily limit for premium tier users.
         */
        private int premiumTierDailyLimit = 1000;

        /**
         * Daily limit for enterprise tier users.
         * -1 means unlimited.
         */
        private int enterpriseTierDailyLimit = -1;

        /**
         * Whether to track usage metrics.
         */
        private boolean trackUsage = true;
    }

    /**
     * Google Cloud Vision specific configuration.
     */
    @Data
    public static class GoogleVisionConfig {
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
         * Supported languages (ISO 639-1 codes).
         */
        private List<String> languages = List.of("en", "es", "fr", "de", "it", "pt", "nl", "pl", "ru", "zh", "ja",
                "ko");

        /**
         * Whether to include confidence scores in results.
         */
        private boolean includeConfidence = true;

        /**
         * Whether to detect and return document language.
         */
        private boolean detectLanguage = true;
    }

    /**
     * Tesseract specific configuration.
     */
    @Data
    public static class TesseractConfig {
        /**
         * Path to Tesseract data files.
         */
        private String dataPath;

        /**
         * Default language for Tesseract OCR.
         */
        private String language = "eng";

        /**
         * Page segmentation mode (PSM).
         * 3 = Fully automatic page segmentation
         */
        private int pageSegMode = 3;

        /**
         * OCR Engine Mode (OEM).
         * 3 = Default, based on what is available
         */
        private int ocrEngineMode = 3;
    }

    /**
     * Check if a provider is enabled.
     */
    public boolean isProviderEnabled(OcrProviderType provider) {
        return enabledProviders.contains(provider);
    }
}
