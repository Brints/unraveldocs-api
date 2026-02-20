package com.extractor.unraveldocs.ai.service;

import com.extractor.unraveldocs.ai.config.AiModelConfig;
import com.extractor.unraveldocs.ai.config.AiProperties;
import com.extractor.unraveldocs.ai.dto.response.ClassifyResponse;
import com.extractor.unraveldocs.ai.provider.AiModelProvider;
import com.extractor.unraveldocs.ai.quota.AiCostResult;
import com.extractor.unraveldocs.ai.quota.AiQuotaService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service for AI-powered document classification and tagging.
 * Classifies documents into types and generates descriptive tags
 * from OCR-extracted text.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClassificationService {

    private final AiModelConfig aiModelConfig;
    private final AiProperties aiProperties;
    private final AiQuotaService aiQuotaService;
    private final OcrDataRepository ocrDataRepository;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitizer;

    private static final String CLASSIFICATION_PROMPT = """
            Analyze the following document text and classify it. Return your response as a JSON object with \
            exactly these fields:
            - "document_type": one of ["invoice", "receipt", "contract", "letter", \
            "id_document", "medical", "legal", "academic", "report", "form", "other"]
            - "tags": an array of 3-5 descriptive keyword tags
            - "confidence": a number between 0 and 1 representing classification confidence
            
            Return ONLY the JSON object, no other text.
            
            Document text:
            %s""";

    /**
     * Classify a document and generate tags from its OCR text.
     *
     * @param userId     The authenticated user's ID
     * @param documentId The document to classify
     * @return ClassifyResponse with document type, tags, and billing info
     */
    @Transactional
    public ClassifyResponse classify(String userId, String documentId) {
        // 1. Load and validate OCR data
        OcrData ocrData = ocrDataRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new NotFoundException(
                        "No OCR data found for document: " + documentId));

        if (ocrData.getStatus() != OcrStatus.COMPLETED) {
            throw new BadRequestException(
                    "OCR processing is not complete for this document. Current status: " + ocrData.getStatus());
        }

        String extractedText = ocrData.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            throw new BadRequestException("No extracted text available for classification.");
        }

        // 2. Check billing
        int creditCost = aiProperties.getClassification().getClassificationCreditCost();
        AiCostResult costResult = aiQuotaService.consumeAiOperation(userId, creditCost);
        if (!costResult.isAllowed()) {
            throw new BadRequestException(costResult.getReason());
        }

        // 3. Determine model provider
        AiModelProvider provider = aiModelConfig.getDefaultProvider();

        // 4. Build prompt and call AI model
        String inputText = truncateText(extractedText);
        String promptText = String.format(CLASSIFICATION_PROMPT, inputText);
        String aiResponse = callAiModel(provider, promptText);

        // 5. Parse AI response
        ClassificationResult result = parseClassificationResponse(aiResponse);

        // 6. Persist on OcrData
        ocrData.setDocumentType(result.documentType());
        ocrData.setAiTags(String.join(",", result.tags()));
        ocrDataRepository.save(ocrData);

        // 7. Build response
        return ClassifyResponse.builder()
                .documentId(documentId)
                .documentType(result.documentType())
                .tags(result.tags())
                .confidence(result.confidence())
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
            //return response.getResult().getOutput().getText();
            return Objects.requireNonNull(response.getResult()).getOutput().getText();
        } catch (Exception e) {
            log.warn("Primary AI model ({}) failed: {}. Attempting fallback...",
                    sanitizer.sanitizeLogging(provider.getDisplayName()), e.getMessage());

            if (aiModelConfig.isFallbackEnabled()) {
                try {
                    AiModelProvider fallback = aiModelConfig.getFallbackProvider();
                    ChatModel fallbackModel = aiModelConfig.getModel(fallback);
                    ChatResponse response = fallbackModel.call(new Prompt(promptText));

                    return Objects.requireNonNull(response.getResult()).getOutput().getText();
                } catch (Exception fallbackEx) {
                    log.error("Fallback AI model also failed: {}", fallbackEx.getMessage());
                    throw new BadRequestException(
                            "AI classification failed. Please try again later.");
                }
            }
            throw new BadRequestException(
                    "AI classification failed and fallback is disabled. Please try again later.");
        }
    }

    /**
     * Parse the AI JSON response into a ClassificationResult.
     */
    private ClassificationResult parseClassificationResponse(String aiResponse) {
        try {
            // Strip markdown code fences if present
            String cleaned = aiResponse.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(cleaned);

            String documentType = root.has("document_type")
                    ? root.get("document_type").asText("other")
                    : "other";

            List<String> tags = new ArrayList<>();
            if (root.has("tags") && root.get("tags").isArray()) {
                for (JsonNode tag : root.get("tags")) {
                    tags.add(tag.asText());
                }
            }

            double confidence = root.has("confidence")
                    ? root.get("confidence").asDouble(0.5)
                    : 0.5;

            return new ClassificationResult(documentType, tags, confidence);

        } catch (Exception e) {
            log.warn("Failed to parse AI classification response: {}. Raw: {}",
                    e.getMessage(), aiResponse);
            // Return defaults instead of failing
            return new ClassificationResult("other",
                    List.of("unclassified"), 0.0);
        }
    }

    /**
     * Truncate text to the configured maximum input length.
     */
    private String truncateText(String text) {
        int maxLength = aiProperties.getMaxInputLength();
        if (text.length() > maxLength) {
            log.debug("Truncating input text from {} to {} characters", text.length(), maxLength);
            return text.substring(0, maxLength);
        }
        return text;
    }

    /**
     * Internal record for parsed classification results.
     */
    private record ClassificationResult(String documentType, List<String> tags, double confidence) {
    }
}
