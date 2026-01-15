package com.extractor.unraveldocs.documents.impl;

import com.extractor.unraveldocs.documents.dto.request.MoveDocumentRequest;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.FileEntryData;
import com.extractor.unraveldocs.documents.interfaces.DocumentMoveService;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.subscription.service.SubscriptionFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of document move service.
 * Premium feature restricted to Starter+ subscriptions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentMoveServiceImpl implements DocumentMoveService {

    private final DocumentCollectionRepository documentCollectionRepository;
    private final SubscriptionFeatureService subscriptionFeatureService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "documentCollection", key = "#request.sourceCollectionId"),
            @CacheEvict(value = "documentCollection", key = "#request.targetCollectionId"),
            @CacheEvict(value = "documentCollections", key = "#userId")
    })
    public DocumentCollectionResponse<FileEntryData> moveDocument(MoveDocumentRequest request, String userId) {
        // Check premium feature access
        subscriptionFeatureService.requireFeatureAccess(userId, SubscriptionFeatureService.Feature.DOCUMENT_MOVE);

        // Validate source and target are different
        if (request.getSourceCollectionId().equals(request.getTargetCollectionId())) {
            throw new IllegalArgumentException("Source and target collections cannot be the same");
        }

        // Find source collection
        DocumentCollection sourceCollection = documentCollectionRepository.findById(request.getSourceCollectionId())
                .orElseThrow(() -> new NotFoundException(
                        "Source collection not found with ID: " + request.getSourceCollectionId()));

        // Verify ownership of source collection
        if (!sourceCollection.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to access the source collection");
        }

        // Find target collection
        DocumentCollection targetCollection = documentCollectionRepository.findById(request.getTargetCollectionId())
                .orElseThrow(() -> new NotFoundException(
                        "Target collection not found with ID: " + request.getTargetCollectionId()));

        // Verify ownership of target collection
        if (!targetCollection.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to access the target collection");
        }

        // Find the document in source collection
        FileEntry fileToMove = sourceCollection.getFiles().stream()
                .filter(file -> file.getDocumentId().equals(request.getDocumentId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Document not found with ID: " + request.getDocumentId() + " in source collection"));

        // Remove from source collection
        List<FileEntry> sourceFiles = new ArrayList<>(sourceCollection.getFiles());
        sourceFiles.removeIf(file -> file.getDocumentId().equals(request.getDocumentId()));
        sourceCollection.setFiles(sourceFiles);

        // Add to target collection
        List<FileEntry> targetFiles = new ArrayList<>(targetCollection.getFiles());
        targetFiles.add(fileToMove);
        targetCollection.setFiles(targetFiles);

        // Save both collections
        documentCollectionRepository.save(sourceCollection);
        documentCollectionRepository.save(targetCollection);

        log.info("Moved document {} from collection {} to collection {} for user {}",
                request.getDocumentId(),
                request.getSourceCollectionId(),
                request.getTargetCollectionId(),
                userId);

        // Build response
        FileEntryData fileEntryData = FileEntryData.builder()
                .documentId(fileToMove.getDocumentId())
                .originalFileName(fileToMove.getOriginalFileName())
                .displayName(fileToMove.getDisplayName())
                .fileSize(fileToMove.getFileSize())
                .fileUrl(fileToMove.getFileUrl())
                .status(fileToMove.getUploadStatus())
                .isEncrypted(fileToMove.isEncrypted())
                .build();

        return DocumentCollectionResponse.<FileEntryData>builder()
                .statusCode(HttpStatus.OK.value())
                .status("success")
                .message("Document moved successfully to collection: " + targetCollection.getName())
                .data(fileEntryData)
                .build();
    }
}
