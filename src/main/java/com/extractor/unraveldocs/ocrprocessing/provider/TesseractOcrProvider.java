package com.extractor.unraveldocs.ocrprocessing.provider;

import com.extractor.unraveldocs.ocrprocessing.config.OcrProperties;
import com.extractor.unraveldocs.ocrprocessing.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Tesseract OCR provider implementation.
 * Uses the local Tesseract engine for OCR processing.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tesseract.datapath")
public class TesseractOcrProvider implements OcrProvider {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/bmp",
            "image/tiff",
            "image/webp");

    private static final List<String> SUPPORTED_LANGUAGES = List.of(
            "eng", "deu", "fra", "spa", "ita", "por", "nld", "pol", "rus", "jpn", "kor", "chi_sim", "chi_tra");

    private final OcrProperties ocrProperties;
    private final String tesseractDataPath;

    public TesseractOcrProvider(OcrProperties ocrProperties) {
        this.ocrProperties = ocrProperties;
        this.tesseractDataPath = ocrProperties.getTesseract().getDataPath();
        log.info("TesseractOcrProvider initialized with datapath: {}", tesseractDataPath);
    }

    @Override
    public OcrProviderType getProviderType() {
        return OcrProviderType.TESSERACT;
    }

    @Override
    public OcrResult extractText(OcrRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate request
            if (!request.isValid()) {
                throw new OcrProcessingException(
                        "Invalid OCR request: no image data provided",
                        OcrProviderType.TESSERACT,
                        request.getDocumentId(),
                        false);
            }

            // Load image
            BufferedImage image = loadImage(request);
            if (image == null) {
                throw new OcrProcessingException(
                        "Failed to load image from source",
                        OcrProviderType.TESSERACT,
                        request.getDocumentId(),
                        false);
            }

            // Configure Tesseract
            Tesseract tesseract = createTesseractInstance(request);

            // Perform OCR
            String extractedText = tesseract.doOCR(image);
            long processingTime = System.currentTimeMillis() - startTime;

            log.debug("Tesseract OCR completed for document {} in {}ms, extracted {} characters",
                    request.getDocumentId(), processingTime, extractedText.length());

            return OcrResult.builder()
                    .extractedText(extractedText)
                    .providerType(OcrProviderType.TESSERACT)
                    .processingTimeMs(processingTime)
                    .documentId(request.getDocumentId())
                    .success(true)
                    .build();

        } catch (OcrProcessingException e) {
            throw e;
        } catch (IOException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Failed to load image for OCR: {}", e.getMessage(), e);
            return OcrResult.failure(e, OcrProviderType.TESSERACT, processingTime)
                    .withMetadata("documentId", request.getDocumentId());
        } catch (TesseractException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Tesseract OCR failed: {}", e.getMessage(), e);
            return OcrResult.failure(e, OcrProviderType.TESSERACT, processingTime)
                    .withMetadata("documentId", request.getDocumentId());
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during Tesseract OCR: {}", e.getMessage(), e);
            return OcrResult.failure(e, OcrProviderType.TESSERACT, processingTime)
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
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public boolean isAvailable() {
        return tesseractDataPath != null && !tesseractDataPath.isBlank();
    }

    @Override
    public long getMaxFileSizeBytes() {
        return ocrProperties.getMaxFileSizeBytes();
    }

    /**
     * Load image from the request (URL or bytes).
     */
    private BufferedImage loadImage(OcrRequest request) throws IOException {
        if (request.hasImageBytes()) {
            return ImageIO.read(new ByteArrayInputStream(request.getImageBytes()));
        } else if (request.hasImageUrl()) {
            URL imageUrl = URI.create(request.getImageUrl()).toURL();
            return ImageIO.read(imageUrl);
        }
        return null;
    }

    /**
     * Create and configure a Tesseract instance.
     */
    private Tesseract createTesseractInstance(OcrRequest request) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractDataPath);

        // Set language
        String language = request.getLanguage();
        if (language == null || language.isBlank()) {
            language = ocrProperties.getTesseract().getLanguage();
        }
        tesseract.setLanguage(language);

        // Set page segmentation mode
        tesseract.setPageSegMode(ocrProperties.getTesseract().getPageSegMode());

        // Set OCR engine mode
        tesseract.setOcrEngineMode(ocrProperties.getTesseract().getOcrEngineMode());

        return tesseract;
    }
}
