package com.extractor.unraveldocs.googlevision.service;

import com.extractor.unraveldocs.googlevision.config.GoogleVisionProperties;
import com.extractor.unraveldocs.ocrprocessing.exception.OcrProcessingException;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrProvider;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrProviderType;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrRequest;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrResult;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Google Cloud Vision OCR provider implementation.
 * Uses Google Cloud Vision API for high-accuracy text extraction.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ocr.google-vision.enabled", havingValue = "true")
public class GoogleVisionOcrProvider implements OcrProvider {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/bmp",
            "image/tiff",
            "image/webp",
            "application/pdf");

    private final ImageAnnotatorClient visionClient;
    private final GoogleVisionProperties properties;
    private final AtomicBoolean isAvailable = new AtomicBoolean(true);

    public GoogleVisionOcrProvider(
            ImageAnnotatorClient visionClient,
            GoogleVisionProperties properties) {
        this.visionClient = visionClient;
        this.properties = properties;
        log.info("GoogleVisionOcrProvider initialized with {} supported languages",
                properties.getLanguages().size());
    }

    @Override
    public OcrProviderType getProviderType() {
        return OcrProviderType.GOOGLE_VISION;
    }

    @Override
    public OcrResult extractText(OcrRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate request
            validateRequest(request);

            // Load image data
            ByteString imageBytes = loadImageBytes(request);

            // Build Vision API request
            Image image = Image.newBuilder()
                    .setContent(imageBytes)
                    .build();

            // Choose feature type based on content
            Feature.Type featureType = determineFeatureType(request);
            Feature feature = Feature.newBuilder()
                    .setType(featureType)
                    .setMaxResults(properties.getFeatures().getMaxResults())
                    .build();

            // Add language hints if specified
            ImageContext.Builder contextBuilder = ImageContext.newBuilder();
            if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
                contextBuilder.addLanguageHints(request.getLanguage());
            }

            AnnotateImageRequest visionRequest = AnnotateImageRequest.newBuilder()
                    .setImage(image)
                    .addFeatures(feature)
                    .setImageContext(contextBuilder.build())
                    .build();

            // Execute Vision API call
            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(
                    List.of(visionRequest));

            // Process response
            return processResponse(response, request, startTime);

        } catch (OcrProcessingException e) {
            throw e;
        } catch (IOException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Failed to load image for Vision API: {}", e.getMessage(), e);
            return OcrResult.failure(e, OcrProviderType.GOOGLE_VISION, processingTime)
                    .withMetadata("documentId", request.getDocumentId());
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Vision API error: {}", e.getMessage(), e);

            // Check if this is a transient error
            if (isTransientError(e)) {
                isAvailable.set(false);
                // TODO: Schedule availability check (in production, use a health check)
            }

            return OcrResult.failure(e, OcrProviderType.GOOGLE_VISION, processingTime)
                    .withMetadata("documentId", request.getDocumentId());
        }
    }

    @Override
    public boolean supports(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    @Override
    public List<String> getSupportedLanguages() {
        return properties.getLanguages();
    }

    @Override
    public boolean isAvailable() {
        return isAvailable.get() && visionClient != null;
    }

    @Override
    public long getMaxFileSizeBytes() {
        return properties.getMaxFileSizeBytes();
    }

    /**
     * Validate the OCR request.
     */
    private void validateRequest(OcrRequest request) {
        if (!request.isValid()) {
            throw new OcrProcessingException(
                    "Invalid OCR request: no image data provided",
                    OcrProviderType.GOOGLE_VISION,
                    request.getDocumentId(),
                    false);
        }

        if (request.getMimeType() != null && !supports(request.getMimeType())) {
            throw OcrProcessingException.unsupportedFileType(
                    request.getMimeType(),
                    OcrProviderType.GOOGLE_VISION);
        }
    }

    /**
     * Load image bytes from URL or direct bytes.
     */
    private ByteString loadImageBytes(OcrRequest request) throws IOException {
        if (request.hasImageBytes()) {
            return ByteString.copyFrom(request.getImageBytes());
        } else if (request.hasImageUrl()) {
            URL imageUrl = URI.create(request.getImageUrl()).toURL();
            try (InputStream is = imageUrl.openStream()) {
                return ByteString.readFrom(is);
            }
        }
        throw new IOException("No image source available");
    }

    /**
     * Determine the best feature type for the content.
     */
    private Feature.Type determineFeatureType(OcrRequest request) {
        // Use DOCUMENT_TEXT_DETECTION for PDFs and dense documents
        if (request.getMimeType() != null) {
            if (request.getMimeType().contains("pdf")) {
                return Feature.Type.DOCUMENT_TEXT_DETECTION;
            }
        }

        // Check metadata for document type hint
        Object docTypeHint = request.getMetadata().get("documentType");
        if ("dense".equals(docTypeHint) || "document".equals(docTypeHint)) {
            return Feature.Type.DOCUMENT_TEXT_DETECTION;
        }

        // Default to standard text detection
        return Feature.Type.TEXT_DETECTION;
    }

    /**
     * Process the Vision API response and build OcrResult.
     */
    private OcrResult processResponse(
            BatchAnnotateImagesResponse response,
            OcrRequest request,
            long startTime) {

        long processingTime = System.currentTimeMillis() - startTime;

        if (response.getResponsesList().isEmpty()) {
            return OcrResult.failure(
                    "Empty response from Vision API",
                    OcrProviderType.GOOGLE_VISION,
                    processingTime).withMetadata("documentId", request.getDocumentId());
        }

        AnnotateImageResponse annotateResponse = response.getResponses(0);

        // Check for errors
        if (annotateResponse.hasError()) {
            String errorMessage = annotateResponse.getError().getMessage();
            log.error("Vision API returned error: {}", errorMessage);
            return OcrResult.failure(
                    errorMessage,
                    OcrProviderType.GOOGLE_VISION,
                    processingTime).withMetadata("documentId", request.getDocumentId());
        }

        // Extract text and metadata
        String extractedText = extractFullText(annotateResponse);
        Double confidence = extractConfidence(annotateResponse);
        String detectedLanguage = extractLanguage(annotateResponse);

        log.debug("Vision API completed for document {} in {}ms, extracted {} characters",
                request.getDocumentId(), processingTime, extractedText.length());

        return OcrResult.builder()
                .extractedText(extractedText)
                .providerType(OcrProviderType.GOOGLE_VISION)
                .processingTimeMs(processingTime)
                .documentId(request.getDocumentId())
                .confidence(confidence)
                .languageDetected(detectedLanguage)
                .success(true)
                .build();
    }

    /**
     * Extract full text from the annotation response.
     */
    private String extractFullText(AnnotateImageResponse response) {
        // Try full text annotation first (from DOCUMENT_TEXT_DETECTION)
        if (response.hasFullTextAnnotation()) {
            return response.getFullTextAnnotation().getText();
        }

        // Fall back to text annotations (from TEXT_DETECTION)
        List<EntityAnnotation> annotations = response.getTextAnnotationsList();
        if (!annotations.isEmpty()) {
            // First annotation contains the full text
            return annotations.getFirst().getDescription();
        }

        return "";
    }

    /**
     * Extract confidence score from the response.
     */
    private Double extractConfidence(AnnotateImageResponse response) {
        if (!properties.isIncludeConfidence()) {
            return null;
        }

        if (response.hasFullTextAnnotation()) {
            TextAnnotation fullText = response.getFullTextAnnotation();
            if (!fullText.getPagesList().isEmpty()) {
                Page page = fullText.getPages(0);
                if (!page.getBlocksList().isEmpty()) {
                    // Average confidence across all blocks
                    double totalConfidence = 0;
                    int blockCount = 0;
                    for (Block block : page.getBlocksList()) {
                        totalConfidence += block.getConfidence();
                        blockCount++;
                    }
                    if (blockCount > 0) {
                        return totalConfidence / blockCount;
                    }
                }
            }
        }

        // Try text annotations
        List<EntityAnnotation> annotations = response.getTextAnnotationsList();
        if (!annotations.isEmpty() && annotations.getFirst().getScore() > 0) {
            return (double) annotations.getFirst().getScore();
        }

        return null;
    }

    /**
     * Extract detected language from the response.
     */
    private String extractLanguage(AnnotateImageResponse response) {
        if (!properties.isDetectLanguage()) {
            return null;
        }

        if (response.hasFullTextAnnotation()) {
            TextAnnotation fullText = response.getFullTextAnnotation();
            if (!fullText.getPagesList().isEmpty()) {
                Page page = fullText.getPages(0);
                if (page.hasProperty() &&
                        !page.getProperty().getDetectedLanguagesList().isEmpty()) {
                    return page.getProperty().getDetectedLanguages(0).getLanguageCode();
                }
            }
        }

        // Try text annotations
        List<EntityAnnotation> annotations = response.getTextAnnotationsList();
        if (!annotations.isEmpty() && !annotations.getFirst().getLocale().isEmpty()) {
            return annotations.getFirst().getLocale();
        }

        return null;
    }

    /**
     * Check if the error is transient and the provider should be marked
     * unavailable.
     */
    private boolean isTransientError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        // Check for common transient errors
        return message.contains("UNAVAILABLE") ||
                message.contains("DEADLINE_EXCEEDED") ||
                message.contains("RESOURCE_EXHAUSTED") ||
                message.contains("rate limit");
    }
}
