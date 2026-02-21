package com.extractor.unraveldocs.ocrprocessing.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class FileResultData {
    private String documentId;
    private String originalFileName;
    private String status;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private String extractedText;
    private String editedContent;
    private String contentFormat;
    private String editedBy;
    private OffsetDateTime editedAt;
    private String aiSummary;
    private String documentType;
    private List<String> aiTags;
}
