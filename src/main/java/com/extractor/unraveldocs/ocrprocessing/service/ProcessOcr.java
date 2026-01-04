package com.extractor.unraveldocs.ocrprocessing.service;

import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.interfaces.ProcessOcrService;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrRequest;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrResult;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.elasticsearch.events.IndexAction;
import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchIndexingService;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Processes OCR requests using the configured OCR provider.
 * Supports both Tesseract and Google Cloud Vision via the provider abstraction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessOcr implements ProcessOcrService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final SanitizeLogging sanitizeLogging;
    private final OcrDataRepository ocrDataRepository;
    private final OcrProcessingService ocrProcessingService;
    private final UserRepository userRepository;
    private final Optional<ElasticsearchIndexingService> elasticsearchIndexingService;

    @Override
    @Transactional
    public void processOcrRequest(String collectionId, String documentId) {
        DocumentCollection collection = documentCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new NotFoundException("Collection not found with ID: " + collectionId));

        FileEntry fileEntry = collection.getFiles().stream()
                .filter(f -> f.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("File not found with document ID: " + documentId));

        OcrData ocrData = ocrDataRepository.findByDocumentId(fileEntry.getDocumentId())
                .orElseThrow(() -> new NotFoundException(
                        "OCR data not found for document ID: " + fileEntry.getDocumentId()));

        if (ocrData.getStatus() == OcrStatus.COMPLETED) {
            log.info("File {} already processed. Skipping.", sanitizeLogging.sanitizeLogging(documentId));
            return;
        }

        ocrData.setStatus(OcrStatus.PROCESSING);
        ocrDataRepository.save(ocrData);

        try {
            log.info("Starting OCR text extraction for document: {}", sanitizeLogging.sanitizeLogging(documentId));

            // Build OCR request using the new abstraction
            OcrRequest ocrRequest = buildOcrRequest(fileEntry, collection);

            // Get user info for quota tracking
            String userId = collection.getUser().getId();
            String userTier = getUserTier(userId);

            // Process using the new provider abstraction
            OcrResult result = ocrProcessingService.processOcr(ocrRequest, userId, userTier);

            // Update OCR data with result
            updateOcrDataFromResult(ocrData, result);

            log.info("OCR text extraction completed for document: {} using provider: {}",
                    sanitizeLogging.sanitizeLogging(documentId),
                    result.getProviderType());

        } catch (Exception e) {
            log.error("OCR processing failed for document {}: {}",
                    sanitizeLogging.sanitizeLogging(documentId), e.getMessage(), e);
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setErrorMessage(e.getMessage());
        } finally {
            updateCollectionStatus(collection);
            ocrDataRepository.save(ocrData);
            documentCollectionRepository.save(collection);

            // Index document in Elasticsearch after OCR completion
            if (ocrData.getStatus() == OcrStatus.COMPLETED) {
                elasticsearchIndexingService.ifPresent(
                        service -> service.indexDocument(collection, fileEntry, ocrData, IndexAction.CREATE));
            }
        }
    }

    /**
     * Build an OCR request from the file entry.
     */
    private OcrRequest buildOcrRequest(FileEntry fileEntry, DocumentCollection collection) {
        return OcrRequest.builder()
                .documentId(fileEntry.getDocumentId())
                .collectionId(collection.getId())
                .imageUrl(fileEntry.getFileUrl())
                .mimeType(fileEntry.getFileType())
                .userId(collection.getUser().getId())
                .fallbackEnabled(true)
                .build();
    }

    /**
     * Update OCR data entity from the result.
     */
    private void updateOcrDataFromResult(OcrData ocrData, OcrResult result) {
        if (result.isSuccess()) {
            ocrData.setExtractedText(result.getExtractedText());
            ocrData.setStatus(OcrStatus.COMPLETED);
            ocrData.setErrorMessage(null);
        } else {
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setErrorMessage(result.getErrorMessage());
        }
    }

    /**
     * Get user subscription tier for quota tracking.
     */
    private String getUserTier(String userId) {
        if (userId == null) {
            return "free";
        }

        try {
            return userRepository.findById(userId)
                    .map(user -> {
                        if (user.getSubscription() != null &&
                                user.getSubscription().getPlan() != null) {
                            return user.getSubscription().getPlan().getName().name().toLowerCase();
                        }
                        return "free";
                    })
                    .orElse("free");
        } catch (Exception e) {
            log.warn("Failed to get user tier for {}, defaulting to free: {}", userId, e.getMessage());
            return "free";
        }
    }

    private void updateCollectionStatus(DocumentCollection collection) {
        List<String> documentIds = collection.getFiles().stream()
                .map(FileEntry::getDocumentId)
                .toList();

        if (documentIds.isEmpty()) {
            collection.setCollectionStatus(DocumentStatus.PROCESSED);
            return;
        }

        Map<String, OcrStatus> statusMap = ocrDataRepository.findByDocumentIdIn(documentIds).stream()
                .collect(Collectors.toMap(OcrData::getDocumentId, OcrData::getStatus));

        long totalFiles = documentIds.size();

        long completedCount = documentIds.stream()
                .filter(id -> statusMap.get(id) == OcrStatus.COMPLETED)
                .count();

        long failedCount = documentIds.stream()
                .filter(id -> statusMap.get(id) == OcrStatus.FAILED)
                .count();

        if (completedCount == totalFiles) {
            collection.setCollectionStatus(DocumentStatus.PROCESSED);
        } else if (completedCount + failedCount == totalFiles) {
            collection.setCollectionStatus(DocumentStatus.FAILED_OCR);
        } else {
            collection.setCollectionStatus(DocumentStatus.PROCESSING);
        }
        log.info("Collection {} status updated to: {}",
                sanitizeLogging.sanitizeLogging(collection.getId()), collection.getCollectionStatus());
    }
}
