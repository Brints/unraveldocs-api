package com.extractor.unraveldocs.ocrprocessing.utils;

import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.dto.request.PdfPageRange;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

@Slf4j
@Component
public class ExtractImageURL {

    /**
     * Extract text from a file URL (image or PDF).
     * For backward compatibility, this overload processes all pages of a PDF.
     */
    public static void extractImageURL(FileEntry fileEntry, OcrData ocrData,
            String tesseractDataPath)
            throws IOException, TesseractException {
        extractImageURL(fileEntry, ocrData, tesseractDataPath, null);
    }

    /**
     * Extract text from a file URL (image or PDF) with optional page range for
     * PDFs.
     *
     * @param fileEntry         the file entry containing the URL
     * @param ocrData           the OCR data entity to update
     * @param tesseractDataPath path to Tesseract data files
     * @param pageRange         optional page range for PDFs (null = all pages)
     */
    public static void extractImageURL(FileEntry fileEntry, OcrData ocrData,
            String tesseractDataPath, PdfPageRange pageRange)
            throws IOException, TesseractException {
        String fileUrl = fileEntry.getFileUrl();

        if (isPdf(fileUrl)) {
            String extractedText = PdfTextExtractor.extractTextFromUrl(
                    fileUrl, pageRange, tesseractDataPath, "eng");
            ocrData.setExtractedText(extractedText);
            ocrData.setStatus(OcrStatus.COMPLETED);
            return;
        }

        // Existing image-based extraction
        URL imageUrl = URI.create(fileUrl).toURL();
        BufferedImage image = ImageIO.read(imageUrl);
        if (image == null) {
            throw new IOException("Failed to read image from URL: " + fileUrl);
        }

        log.info("Initializing Tesseract with datapath: {}", tesseractDataPath);
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractDataPath);
        tesseract.setLanguage("eng");
        log.info("Tesseract initialized successfully. Performing OCR on image...");

        String extractedText = tesseract.doOCR(image);
        log.info("Tesseract OCR completed, extracted {} characters",
                extractedText != null ? extractedText.length() : 0);

        ocrData.setExtractedText(extractedText);
        ocrData.setStatus(OcrStatus.COMPLETED);
    }

    /**
     * Check if a URL points to a PDF file.
     */
    private static boolean isPdf(String url) {
        if (url == null) {
            return false;
        }
        // Remove query parameters before checking extension
        String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        return path.toLowerCase().endsWith(".pdf");
    }
}
