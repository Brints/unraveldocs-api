package com.extractor.unraveldocs.ai.dto.request;

import com.extractor.unraveldocs.ai.datamodel.SummaryType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AI document summarization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummarizeRequest {

    /**
     * The document ID containing the OCR text to summarize.
     */
    @NotBlank(message = "Document ID is required")
    private String documentId;

    /**
     * The type of summary to generate (SHORT or DETAILED).
     * Defaults to SHORT if not specified.
     */
    private SummaryType summaryType = SummaryType.SHORT;

    /**
     * Optional model preference: "openai" or "mistral".
     * If null, defaults to the configured default provider.
     */
    private String modelPreference;
}
