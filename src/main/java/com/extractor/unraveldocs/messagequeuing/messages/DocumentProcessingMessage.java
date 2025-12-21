package com.extractor.unraveldocs.messagequeuing.messages;

import java.time.Instant;
import java.util.Map;

/**
 * Message for document processing events.
 * Used to queue OCR/document extraction operations.
 *
 * @param documentId The document ID in the system
 * @param userId Owner of the document
 * @param s3Key S3 key where the document is stored
 * @param bucketName S3 bucket name
 * @param fileName Original file name
 * @param mimeType Document MIME type (e.g., application/pdf)
 * @param processingType Type of processing required
 * @param metadata Additional processing metadata
 * @param requestedAt When processing was requested
 * @param priority Processing priority
 */
public record DocumentProcessingMessage(
        String documentId,
        String userId,
        String s3Key,
        String bucketName,
        String fileName,
        String mimeType,
        ProcessingType processingType,
        Map<String, String> metadata,
        Instant requestedAt,
        ProcessingPriority priority
) {
    
    public enum ProcessingType {
        OCR_EXTRACTION,
        TEXT_EXTRACTION,
        IMAGE_ANALYSIS,
        PDF_CONVERSION,
        THUMBNAIL_GENERATION
    }
    
    public enum ProcessingPriority {
        HIGH, NORMAL, LOW
    }
    
    /**
     * Create an OCR extraction message.
     */
    public static DocumentProcessingMessage forOcr(
            String documentId,
            String userId,
            String s3Key,
            String bucketName,
            String fileName,
            String mimeType
    ) {
        return new DocumentProcessingMessage(
                documentId,
                userId,
                s3Key,
                bucketName,
                fileName,
                mimeType,
                ProcessingType.OCR_EXTRACTION,
                Map.of(),
                Instant.now(),
                ProcessingPriority.NORMAL
        );
    }
    
    /**
     * Create a text extraction message.
     */
    public static DocumentProcessingMessage forTextExtraction(
            String documentId,
            String userId,
            String s3Key,
            String bucketName,
            String fileName,
            String mimeType
    ) {
        return new DocumentProcessingMessage(
                documentId,
                userId,
                s3Key,
                bucketName,
                fileName,
                mimeType,
                ProcessingType.TEXT_EXTRACTION,
                Map.of(),
                Instant.now(),
                ProcessingPriority.NORMAL
        );
    }
    
    /**
     * Add metadata.
     */
    public DocumentProcessingMessage withMetadata(Map<String, String> metadata) {
        return new DocumentProcessingMessage(
                documentId,
                userId,
                s3Key,
                bucketName,
                fileName,
                mimeType,
                processingType,
                metadata,
                requestedAt,
                priority
        );
    }
    
    /**
     * Set as high priority.
     */
    public DocumentProcessingMessage highPriority() {
        return new DocumentProcessingMessage(
                documentId,
                userId,
                s3Key,
                bucketName,
                fileName,
                mimeType,
                processingType,
                metadata,
                requestedAt,
                ProcessingPriority.HIGH
        );
    }
}
