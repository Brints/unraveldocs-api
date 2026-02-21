package com.extractor.unraveldocs.ocrprocessing.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Represents page selection for PDF OCR processing.
 * Supports two modes:
 * <ul>
 * <li><b>Range</b>: contiguous range via startPage/endPage (e.g., pages
 * 3–5)</li>
 * <li><b>Discrete</b>: specific pages via pages list (e.g., pages 3, 8,
 * 16)</li>
 * </ul>
 * All page numbers are 1-indexed. If no selection is specified, all pages are
 * processed.
 * When both range and discrete pages are provided, discrete pages take
 * priority.
 */
@Data
@NoArgsConstructor
public class PdfPageRange {
    /**
     * Start page (1-indexed, inclusive). Used for range mode.
     */
    private Integer startPage;

    /**
     * End page (1-indexed, inclusive). Used for range mode.
     */
    private Integer endPage;

    /**
     * Specific pages to process (1-indexed). Used for discrete mode.
     * Takes priority over startPage/endPage if both are provided.
     */
    private List<Integer> pages;

    /**
     * Constructor for range-based selection.
     */
    public PdfPageRange(Integer startPage, Integer endPage) {
        this.startPage = startPage;
        this.endPage = endPage;
    }

    /**
     * Constructor for discrete page selection.
     */
    public PdfPageRange(List<Integer> pages) {
        this.pages = pages;
    }

    /**
     * Check if any page selection is specified.
     */
    public boolean hasSelection() {
        return startPage != null || endPage != null || (pages != null && !pages.isEmpty());
    }

    /**
     * Check if this is a discrete page selection.
     */
    public boolean isDiscrete() {
        return pages != null && !pages.isEmpty();
    }

    /**
     * Validate the page selection against the total number of pages.
     *
     * @param totalPages total number of pages in the PDF
     * @throws IllegalArgumentException if the selection is invalid
     */
    public void validate(int totalPages) {
        if (isDiscrete()) {
            for (int page : pages) {
                if (page < 1) {
                    throw new IllegalArgumentException("Page numbers must be >= 1, got: " + page);
                }
                if (page > totalPages) {
                    throw new IllegalArgumentException(
                            "Page " + page + " exceeds total pages (" + totalPages + ")");
                }
            }
            return;
        }

        // Range validation
        if (startPage != null && startPage < 1) {
            throw new IllegalArgumentException("startPage must be >= 1, got: " + startPage);
        }
        if (endPage != null && endPage < 1) {
            throw new IllegalArgumentException("endPage must be >= 1, got: " + endPage);
        }
        if (startPage != null && endPage != null && startPage > endPage) {
            throw new IllegalArgumentException(
                    "startPage (" + startPage + ") must be <= endPage (" + endPage + ")");
        }
        if (startPage != null && startPage > totalPages) {
            throw new IllegalArgumentException(
                    "startPage (" + startPage + ") exceeds total pages (" + totalPages + ")");
        }
        if (endPage != null && endPage > totalPages) {
            throw new IllegalArgumentException(
                    "endPage (" + endPage + ") exceeds total pages (" + totalPages + ")");
        }
    }

    /**
     * Get the list of 0-indexed page numbers to process.
     *
     * @param totalPages total number of pages in the PDF
     * @return sorted list of 0-indexed page numbers
     */
    public List<Integer> getEffectivePages(int totalPages) {
        if (isDiscrete()) {
            // Convert 1-indexed to 0-indexed, sort, and deduplicate
            return pages.stream()
                    .map(p -> p - 1)
                    .distinct()
                    .sorted()
                    .toList();
        }

        // Range mode
        int start = (startPage != null ? startPage : 1) - 1;
        int end = endPage != null ? endPage : totalPages;
        return IntStream.range(start, end).boxed().toList();
    }

    /**
     * Get effective start page (0-indexed for PDFBox). Only for range mode.
     */
    public int getEffectiveStartPage() {
        return (startPage != null ? startPage : 1) - 1;
    }

    /**
     * Get effective end page (0-indexed, exclusive). Only for range mode.
     */
    public int getEffectiveEndPage(int totalPages) {
        return endPage != null ? endPage : totalPages;
    }
}
