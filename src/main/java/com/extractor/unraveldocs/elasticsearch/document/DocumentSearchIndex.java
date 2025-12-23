package com.extractor.unraveldocs.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch document for indexing documents and their OCR content.
 * Combines document metadata from DocumentCollection/FileEntry with
 * extracted text from OcrData.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "documents")
@Setting(settingPath = "/elasticsearch/document-settings.json")
public class DocumentSearchIndex {

    @Id
    private String id;

    /**
     * User ID who owns this document.
     */
    @Field(type = FieldType.Keyword)
    private String userId;

    /**
     * Document collection ID.
     */
    @Field(type = FieldType.Keyword)
    private String collectionId;

    /**
     * Original file name (searchable).
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String fileName;

    /**
     * File type (e.g., pdf, docx, png).
     */
    @Field(type = FieldType.Keyword)
    private String fileType;

    /**
     * File size in bytes.
     */
    @Field(type = FieldType.Long)
    private Long fileSize;

    /**
     * Current status of the document (e.g., PENDING, COMPLETED, FAILED).
     */
    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * OCR processing status.
     */
    @Field(type = FieldType.Keyword)
    private String ocrStatus;

    /**
     * Extracted text content from OCR.
     * This is the main searchable field with full-text analysis.
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String extractedText;

    /**
     * File URL in storage.
     */
    @Field(type = FieldType.Keyword, index = false)
    private String fileUrl;

    /**
     * Timestamp when the document was uploaded.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime uploadTimestamp;

    /**
     * Timestamp when the document was created in the system.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the document was last updated.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime updatedAt;

    /**
     * Tags for categorization (future use).
     */
    @Field(type = FieldType.Keyword)
    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
