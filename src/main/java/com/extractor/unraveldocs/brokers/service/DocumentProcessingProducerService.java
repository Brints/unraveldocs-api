package com.extractor.unraveldocs.brokers.service;

import com.extractor.unraveldocs.brokers.core.Message;
import com.extractor.unraveldocs.brokers.core.MessageBrokerFactory;
import com.extractor.unraveldocs.brokers.core.MessageResult;
import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.brokers.messages.DocumentProcessingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for producing document processing messages to Kafka.
 * Provides a high-level API for queueing document processing jobs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingProducerService {
    
    private final MessageBrokerFactory messageBrokerFactory;
    
    /**
     * Queue a document for OCR processing.
     *
     * @param documentId Document ID
     * @param userId Owner user ID
     * @param s3Key S3 key where document is stored
     * @param bucketName S3 bucket name
     * @param fileName Original file name
     * @param mimeType Document MIME type
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> queueForOcr(
            String documentId,
            String userId,
            String s3Key,
            String bucketName,
            String fileName,
            String mimeType
    ) {
        DocumentProcessingMessage message = DocumentProcessingMessage.forOcr(
                documentId,
                userId,
                s3Key,
                bucketName,
                fileName,
                mimeType
        );
        
        return sendProcessingMessage(message);
    }
    
    /**
     * Queue a document for text extraction.
     *
     * @param documentId Document ID
     * @param userId Owner user ID
     * @param s3Key S3 key where document is stored
     * @param bucketName S3 bucket name
     * @param fileName Original file name
     * @param mimeType Document MIME type
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> queueForTextExtraction(
            String documentId,
            String userId,
            String s3Key,
            String bucketName,
            String fileName,
            String mimeType
    ) {
        DocumentProcessingMessage message = DocumentProcessingMessage.forTextExtraction(
                documentId,
                userId,
                s3Key,
                bucketName,
                fileName,
                mimeType
        );
        
        return sendProcessingMessage(message);
    }
    
    /**
     * Queue a high priority document processing job.
     *
     * @param documentId Document ID
     * @param userId Owner user ID
     * @param s3Key S3 key where document is stored
     * @param bucketName S3 bucket name
     * @param fileName Original file name
     * @param mimeType Document MIME type
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> queueHighPriorityOcr(
            String documentId,
            String userId,
            String s3Key,
            String bucketName,
            String fileName,
            String mimeType
    ) {
        DocumentProcessingMessage message = DocumentProcessingMessage.forOcr(
                documentId,
                userId,
                s3Key,
                bucketName,
                fileName,
                mimeType
        ).highPriority();
        
        return sendProcessingMessage(message);
    }
    
    /**
     * Queue a document processing message with custom metadata.
     *
     * @param documentId Document ID
     * @param userId Owner user ID
     * @param s3Key S3 key where document is stored
     * @param bucketName S3 bucket name
     * @param fileName Original file name
     * @param mimeType Document MIME type
     * @param metadata Custom metadata
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> queueForOcrWithMetadata(
            String documentId,
            String userId,
            String s3Key,
            String bucketName,
            String fileName,
            String mimeType,
            Map<String, String> metadata
    ) {
        DocumentProcessingMessage message = DocumentProcessingMessage.forOcr(
                documentId,
                userId,
                s3Key,
                bucketName,
                fileName,
                mimeType
        ).withMetadata(metadata);
        
        return sendProcessingMessage(message);
    }
    
    /**
     * Send a document processing message to Kafka.
     */
    private CompletableFuture<MessageResult> sendProcessingMessage(DocumentProcessingMessage docMessage) {
        log.debug("Queueing document for processing. DocId: {}, Type: {}",
                docMessage.documentId(), docMessage.processingType());
        
        Message<DocumentProcessingMessage> message = Message.of(
                docMessage,
                KafkaTopicConfig.TOPIC_DOCUMENTS,
                docMessage.userId() // Use user ID as partition key for ordering per user
        );
        
        return messageBrokerFactory.<DocumentProcessingMessage>getDefaultProducer()
                .send(message)
                .thenApply(result -> {
                    if (result.success()) {
                        log.info("Document queued for processing. DocId: {}, MessageId: {}",
                                docMessage.documentId(), result.messageId());
                    } else {
                        log.warn("Failed to queue document for processing. DocId: {}, Error: {}",
                                docMessage.documentId(), result.errorMessage());
                    }
                    return result;
                });
    }
}
