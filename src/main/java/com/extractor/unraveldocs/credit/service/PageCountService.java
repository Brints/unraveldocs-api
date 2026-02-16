package com.extractor.unraveldocs.credit.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * Service for calculating page counts from uploaded files.
 * Used to determine how many credits are needed to process documents.
 * 1 credit = 1 page.
 */
@Slf4j
@Service
public class PageCountService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "tiff", "tif", "bmp", "gif", "webp");

    private static final String PDF_EXTENSION = "pdf";

    /**
     * Calculate the total number of pages across all uploaded files.
     * PDF files: count actual pages.
     * Image files: 1 page each.
     * Other files: 1 page each.
     *
     * @param files Array of uploaded files
     * @return Total page count
     */
    public int calculatePageCount(MultipartFile[] files) {
        int totalPages = 0;

        for (MultipartFile file : files) {
            String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "unknown");
            String extension = getFileExtension(filename).toLowerCase();

            if (PDF_EXTENSION.equals(extension)) {
                totalPages += countPdfPages(file, filename);
            } else if (IMAGE_EXTENSIONS.contains(extension)) {
                totalPages += 1;
            } else {
                // Other supported file types count as 1 page
                totalPages += 1;
            }
        }

        return totalPages;
    }

    /**
     * Count the number of pages in a PDF file using Apache PDFBox.
     */
    private int countPdfPages(MultipartFile file, String filename) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            int pages = document.getNumberOfPages();
            log.debug("PDF '{}' has {} pages", filename, pages);
            return pages;
        } catch (IOException e) {
            log.warn("Failed to count pages for PDF '{}', defaulting to 1: {}", filename, e.getMessage());
            return 1;
        }
    }

    /**
     * Extract the file extension from a filename.
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
