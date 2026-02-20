package com.extractor.unraveldocs.ai.quota;

import lombok.Builder;
import lombok.Data;

/**
 * Result of an AI quota consumption attempt.
 * Contains whether the operation was allowed and billing details.
 */
@Data
@Builder
public class AiCostResult {

    /**
     * Whether the AI operation is allowed.
     */
    private boolean allowed;

    /**
     * The billing source used: "subscription", "credits", or null if denied.
     */
    private String source;

    /**
     * Human-readable reason (used when denied).
     */
    private String reason;

    /**
     * Number of credits charged (0 if billed against subscription allowance).
     */
    private int creditsCharged;

    public static AiCostResult fromSubscription() {
        return AiCostResult.builder()
                .allowed(true)
                .source("subscription")
                .creditsCharged(0)
                .build();
    }

    public static AiCostResult fromCredits(int creditsCharged) {
        return AiCostResult.builder()
                .allowed(true)
                .source("credits")
                .creditsCharged(creditsCharged)
                .build();
    }

    public static AiCostResult denied(String reason) {
        return AiCostResult.builder()
                .allowed(false)
                .reason(reason)
                .creditsCharged(0)
                .build();
    }
}
