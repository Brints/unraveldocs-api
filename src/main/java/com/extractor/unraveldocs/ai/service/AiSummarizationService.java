package com.extractor.unraveldocs.ai.service;

import com.extractor.unraveldocs.ai.config.AiModelConfig;
import com.extractor.unraveldocs.ai.config.AiProperties;
import com.extractor.unraveldocs.ai.datamodel.SummaryType;
import com.extractor.unraveldocs.ai.dto.request.SummarizeRequest;
import com.extractor.unraveldocs.ai.dto.response.SummarizeResponse;
import com.extractor.unraveldocs.ai.provider.AiModelProvider;
import com.extractor.unraveldocs.ai.quota.AiCostResult;
import com.extractor.unraveldocs.ai.quota.AiQuotaService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Service for AI-powered document summarization.
 * Uses the configured AI model (OpenAI/Mistral) to generate summaries
 * of OCR-extracted text, with automatic fallback support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummarizationService {

    private final AiModelConfig aiModelConfig;
    private final AiProperties aiProperties;
    private final AiQuotaService aiQuotaService;
    private final OcrDataRepository ocrDataRepository;

    private static final String SHORT_SUMMARY_PROMPT = """
            Summarize the following document text in 1-2 concise sentences. \
            Focus on the main topic and key takeaway.
            
            Document text:
            %s""";

    private static final String DETAILED_SUMMARY_PROMPT = """
            Provide a detailed summary of the following document. \
            Include:
            - A brief overview (1-2 sentences)
            - Key points as a bullet list
            - Any notable details, dates, or figures mentioned
            
            Document text:
            %s""";

    /**
     * Summarize the OCR text of a document.
     *
     * @param userId  The authenticated user's ID
     * @param request The summarization request containing documentId and
     *                summaryType
     * @return SummarizeResponse with the generated summary and billing info
     */
    @Transactional
    public SummarizeResponse summarize(String userId, SummarizeRequest request) {
        // 1. Load and validate OCR data
        OcrData ocrData = ocrDataRepository.findByDocumentId(request.getDocumentId())
                .orElseThrow(() -> new NotFoundException(
                        "No OCR data found for document: " + request.getDocumentId()));

        if (ocrData.getStatus() != OcrStatus.COMPLETED) {
            throw new BadRequestException(
                    "OCR processing is not complete for this document. Current status: " + ocrData.getStatus());
        }

        String extractedText = ocrData.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            throw new BadRequestException("No extracted text available for summarization.");
        }

        // 2. Check billing (subscription allowance → credits → denied)
        SummaryType summaryType = request.getSummaryType() != null
                ? request.getSummaryType()
                : SummaryType.SHORT;

        int creditCost = summaryType == SummaryType.DETAILED
                ? aiProperties.getSummarization().getDetailedSummaryCreditCost()
                : aiProperties.getSummarization().getShortSummaryCreditCost();

        AiCostResult costResult = aiQuotaService.consumeAiOperation(userId, creditCost);
        if (!costResult.isAllowed()) {
            throw new BadRequestException(costResult.getReason());
        }

        // 3. Determine model provider
        AiModelProvider provider = request.getModelPreference() != null
                ? AiModelProvider.fromKey(request.getModelPreference())
                : aiModelConfig.getDefaultProvider();

        // 4. Truncate text if needed
        String inputText = truncateText(extractedText);

        // 5. Build prompt and call AI model
        String promptText = buildPrompt(summaryType, inputText);
        String summary = callAiModel(provider, promptText);

        // 6. Persist summary on OcrData
        ocrData.setAiSummary(summary);
        ocrDataRepository.save(ocrData);

        // 7. Build response
        return SummarizeResponse.builder()
                .documentId(request.getDocumentId())
                .summary(summary)
                .summaryType(summaryType)
                .modelUsed(provider.getDisplayName())
                .creditsCharged(costResult.getCreditsCharged())
                .billingSource(costResult.getSource())
                .build();
    }

    /**
     * Call the AI model with fallback support.
     */
    private String callAiModel(AiModelProvider provider, String promptText) {
        try {
            ChatModel model = aiModelConfig.getModel(provider);
            ChatResponse response = model.call(new Prompt(promptText));

            return Objects.requireNonNull(response.getResult()).getOutput().getText();
        } catch (Exception e) {

            if (aiModelConfig.isFallbackEnabled()) {
                try {
                    AiModelProvider fallback = aiModelConfig.getFallbackProvider();
                    ChatModel fallbackModel = aiModelConfig.getModel(fallback);
                    ChatResponse response = fallbackModel.call(new Prompt(promptText));

                    return Objects.requireNonNull(response.getResult()).getOutput().getText();
                } catch (Exception fallbackEx) {
                    throw new BadRequestException(
                            "AI summarization failed. Please try again later.");
                }
            }
            throw new BadRequestException(
                    "AI summarization failed and fallback is disabled. Please try again later.");
        }
    }

    /**
     * Build the prompt text based on summary type.
     */
    private String buildPrompt(SummaryType summaryType, String text) {
        return switch (summaryType) {
            case SHORT -> String.format(SHORT_SUMMARY_PROMPT, text);
            case DETAILED -> String.format(DETAILED_SUMMARY_PROMPT, text);
        };
    }

    /**
     * Truncate text to the configured maximum input length.
     */
    private String truncateText(String text) {
        int maxLength = aiProperties.getMaxInputLength();
        if (text.length() > maxLength) {
            return text.substring(0, maxLength);
        }
        return text;
    }
}
