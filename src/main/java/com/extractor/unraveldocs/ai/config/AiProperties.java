package com.extractor.unraveldocs.ai.config;

import com.extractor.unraveldocs.ai.provider.AiModelProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AI processing.
 * Mirrors the pattern used in OcrProperties for externalized configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * The default AI model provider to use.
     */
    private AiModelProvider defaultProvider = AiModelProvider.OPENAI;

    /**
     * The fallback provider when the primary provider fails.
     */
    private AiModelProvider fallbackProvider = AiModelProvider.MISTRAL_AI;

    /**
     * Whether to enable fallback to another provider if the primary fails.
     */
    private boolean fallbackEnabled = true;

    /**
     * Timeout for AI API calls in seconds.
     */
    private int timeoutSeconds = 60;

    /**
     * Maximum number of retries for failed AI requests.
     */
    private int maxRetries = 2;

    /**
     * Maximum input text length (in characters) to send to the AI model.
     * Longer texts will be truncated.
     */
    private int maxInputLength = 30000;

    /**
     * AI quota configuration per subscription tier.
     */
    private QuotaConfig quota = new QuotaConfig();

    /**
     * Summarization-specific settings.
     */
    private SummarizationConfig summarization = new SummarizationConfig();

    /**
     * Classification-specific settings.
     */
    private ClassificationConfig classification = new ClassificationConfig();

    /**
     * AI quota configuration controlling per-tier monthly allowances.
     */
    @Data
    public static class QuotaConfig {
        /**
         * Whether AI quota enforcement is enabled.
         */
        private boolean enabled = true;

        /**
         * Monthly AI operations limit for Free tier.
         */
        private int freeTierMonthlyLimit = 5;

        /**
         * Monthly AI operations limit for Starter tier.
         */
        private int starterTierMonthlyLimit = 50;

        /**
         * Monthly AI operations limit for Pro tier.
         */
        private int proTierMonthlyLimit = 200;

        /**
         * Monthly AI operations limit for Business tier.
         * -1 means unlimited.
         */
        private int businessTierMonthlyLimit = 500;
    }

    /**
     * Settings for AI summarization.
     */
    @Data
    public static class SummarizationConfig {
        /**
         * Credit cost for a short summary.
         */
        private int shortSummaryCreditCost = 1;

        /**
         * Credit cost for a detailed summary.
         */
        private int detailedSummaryCreditCost = 2;

        /**
         * Temperature for summarization requests (lower = more deterministic).
         */
        private double temperature = 0.3;
    }

    /**
     * Settings for AI classification and tagging.
     */
    @Data
    public static class ClassificationConfig {
        /**
         * Credit cost for document classification.
         */
        private int classificationCreditCost = 1;

        /**
         * Temperature for classification requests (lower = more deterministic).
         */
        private double temperature = 0.2;
    }
}
