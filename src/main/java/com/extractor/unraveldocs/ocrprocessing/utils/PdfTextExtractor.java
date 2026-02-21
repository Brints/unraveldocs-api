package com.extractor.unraveldocs.ocrprocessing.utils;

import com.extractor.unraveldocs.ocrprocessing.dto.request.PdfPageRange;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Utility for extracting text from PDF files.
 * <p>
 * Strategy:
 * <ol>
 * <li>Attempt direct text extraction via PDFTextStripper (for text-based
 * PDFs)</li>
 * <li>If no text found (scanned/image PDF), render pages as images and OCR with
 * Tesseract</li>
 * </ol>
 * Supports optional page selection — either a contiguous range or discrete
 * pages.
 */
@Slf4j
public final class PdfTextExtractor {

    private static final int PDF_RENDER_DPI = 300;

    private PdfTextExtractor() {}

    /**
     * Extract text from a PDF URL with optional page selection.
     */
    public static String extractTextFromUrl(String pdfUrl, PdfPageRange pageRange,
            String tesseractDataPath, String language)
            throws IOException, TesseractException {
        try (InputStream is = URI.create(pdfUrl).toURL().openStream()) {
            byte[] pdfBytes = is.readAllBytes();
            return extractTextFromBytes(pdfBytes, pageRange, tesseractDataPath, language);
        }
    }

    /**
     * Extract text from PDF bytes with optional page selection.
     */
    public static String extractTextFromBytes(byte[] pdfBytes, PdfPageRange pageRange,
            String tesseractDataPath, String language)
            throws IOException, TesseractException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();
            log.info("PDF loaded with {} pages", totalPages);

            if (pageRange != null && pageRange.hasSelection()) {
                pageRange.validate(totalPages);
            }

            // Resolve which pages to process (0-indexed)
            List<Integer> pagesToProcess = resolvePages(pageRange, totalPages);

            // Try direct text extraction first
            String directText = extractDirectText(document, pagesToProcess, totalPages);
            if (directText != null && !directText.isBlank()) {
                log.info("Direct text extraction successful");
                return directText;
            }

            // Fallback to OCR for scanned/image PDFs
            log.info("No direct text found, falling back to OCR for scanned PDF");
            return extractTextViaOcr(document, pagesToProcess, totalPages,
                    tesseractDataPath, language);
        }
    }

    /**
     * Resolve which 0-indexed pages to process.
     */
    private static List<Integer> resolvePages(PdfPageRange pageRange, int totalPages) {
        if (pageRange == null || !pageRange.hasSelection()) {
            // All pages
            return java.util.stream.IntStream.range(0, totalPages).boxed().toList();
        }
        return pageRange.getEffectivePages(totalPages);
    }

    /**
     * Try direct text extraction using PDFTextStripper for the specified pages.
     */
    private static String extractDirectText(PDDocument document, List<Integer> pagesToProcess, int totalPages) throws IOException {
        log.info("Attempting direct text extraction for {} pages", totalPages);
        StringBuilder combinedText = new StringBuilder();
        for (int pageIndex : pagesToProcess) {
            PDFTextStripper stripper = new PDFTextStripper();
            // PDFTextStripper uses 1-indexed pages
            stripper.setStartPage(pageIndex + 1);
            stripper.setEndPage(pageIndex + 1);
            String pageText = stripper.getText(document);

            if (pageText != null && !pageText.isBlank()) {
                if (!combinedText.isEmpty()) {
                    combinedText.append("\n--- Page ").append(pageIndex + 1).append(" ---\n");
                }
                combinedText.append(pageText.strip());
            }
        }

        return combinedText.toString();
    }

    /**
     * Render specified PDF pages to images and OCR with Tesseract.
     */
    private static String extractTextViaOcr(PDDocument document, List<Integer> pagesToProcess,
            int totalPages, String tesseractDataPath,
            String language)
            throws IOException, TesseractException {
        PDFRenderer renderer = new PDFRenderer(document);

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractDataPath);
        tesseract.setLanguage(language != null ? language : "eng");

        StringBuilder combinedText = new StringBuilder();

        for (int pageIndex : pagesToProcess) {
            log.debug("OCR processing PDF page {} of {}", pageIndex + 1, totalPages);
            BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, PDF_RENDER_DPI);
            String pageText = tesseract.doOCR(pageImage);

            if (!combinedText.isEmpty()) {
                combinedText.append("\n--- Page ").append(pageIndex + 1).append(" ---\n");
            }
            combinedText.append(pageText.strip());
        }

        return combinedText.toString();
    }
}
