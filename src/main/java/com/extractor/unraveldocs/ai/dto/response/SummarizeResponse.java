package com.extractor.unraveldocs.ai.dto.response;

import com.extractor.unraveldocs.ai.datamodel.SummaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for AI document summarization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummarizeResponse {

    /**
     * The document ID that was summarized.
     */
    private String documentId;

    /**
     * The AI-generated summary.
     */
    private String summary;

    /**
     * The type of summary generated.
     */
    private SummaryType summaryType;

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
