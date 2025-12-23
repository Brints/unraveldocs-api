package com.extractor.unraveldocs.elasticsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Document-specific search result with highlighted text snippets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResult {

    /**
     * Document ID.
     */
    private String id;

    /**
     * Document collection ID.
     */
    private String collectionId;

    /**
     * Original file name.
     */
    private String fileName;

    /**
     * File type (pdf, docx, etc.).
     */
    private String fileType;

    /**
     * File size in bytes.
     */
    private Long fileSize;

    /**
     * Document status.
     */
    private String status;

    /**
     * OCR processing status.
     */
    private String ocrStatus;

    /**
     * Preview of extracted text (first N characters).
     */
    private String textPreview;

    /**
     * Highlighted text snippets matching the search query.
     */
    @Builder.Default
    private List<String> highlights = new ArrayList<>();

    /**
     * File URL for download.
     */
    private String fileUrl;

    /**
     * Upload timestamp.
     */
    private OffsetDateTime uploadTimestamp;

    /**
     * Creation timestamp.
     */
    private OffsetDateTime createdAt;

    /**
     * Relevance score from Elasticsearch.
     */
    private Float score;
}
