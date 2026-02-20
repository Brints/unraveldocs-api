package com.extractor.unraveldocs.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for AI document classification and tagging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyResponse {

    /**
     * The document ID that was classified.
     */
    private String documentId;

    /**
     * The classified document type (e.g., "invoice", "contract", "receipt").
     */
    private String documentType;

    /**
     * AI-generated descriptive tags.
     */
    private List<String> tags;

    /**
     * Confidence score for the classification (0.0 to 1.0).
     */
    private double confidence;

    /**
     * The AI model provider that was used.
     */
    private String modelUsed;

    /**
     * Number of credits charged for this operation.
     */
    private int creditsCharged;

    /**
     * The billing source: "subscription" or "credits".
     */
    private String billingSource;
}
