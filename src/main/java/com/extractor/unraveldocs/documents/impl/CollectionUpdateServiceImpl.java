package com.extractor.unraveldocs.documents.impl;

import com.extractor.unraveldocs.documents.dto.request.UpdateCollectionRequest;
import com.extractor.unraveldocs.documents.dto.request.UpdateDocumentRequest;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.FileEntryData;
import com.extractor.unraveldocs.documents.dto.response.GetDocumentCollectionData;
import com.extractor.unraveldocs.documents.interfaces.CollectionUpdateService;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of collection and document name update service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionUpdateServiceImpl implements CollectionUpdateService {

    private final DocumentCollectionRepository documentCollectionRepository;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "documentCollection", key = "#collectionId"),
            @CacheEvict(value = "documentCollections", key = "#userId")
    })
    public DocumentCollectionResponse<GetDocumentCollectionData> updateCollectionName(
            String collectionId, UpdateCollectionRequest request, String userId) {

        // Find collection
        DocumentCollection collection = documentCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new NotFoundException("Collection not found with ID: " + collectionId));

        // Verify ownership
        if (!collection.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to update this collection");
        }

        // Check for duplicate name for this user
        boolean nameExists = documentCollectionRepository.findAllByUserId(userId).stream()
                .anyMatch(c -> !c.getId().equals(collectionId) && c.getName().equals(request.getName()));

        if (nameExists) {
            throw new ConflictException("A collection with this name already exists");
        }

        // Update collection name
        String oldName = collection.getName();
        collection.setName(request.getName());
        DocumentCollection savedCollection = documentCollectionRepository.save(collection);

        log.info("Updated collection {} name from '{}' to '{}' for user {}",
                collectionId, oldName, request.getName(), userId);

        // Build response
        List<FileEntryData> fileEntryDataList = savedCollection.getFiles().stream()
                .map(this::mapToFileEntryData)
                .collect(Collectors.toList());

        GetDocumentCollectionData responseData = GetDocumentCollectionData.builder()
                .id(savedCollection.getId())
                .name(savedCollection.getName())
                .userId(savedCollection.getUser().getId())
                .collectionStatus(savedCollection.getCollectionStatus())
                .uploadTimestamp(savedCollection.getUploadTimestamp())
                .createdAt(savedCollection.getCreatedAt())
                .updatedAt(savedCollection.getUpdatedAt())
                .files(fileEntryDataList)
                .build();

        return DocumentCollectionResponse.<GetDocumentCollectionData>builder()
                .statusCode(HttpStatus.OK.value())
                .status("success")
                .message("Collection name updated successfully")
                .data(responseData)
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "documentCollection", key = "#collectionId"),
            @CacheEvict(value = "fileEntry", key = "#collectionId + '-' + #documentId")
    })
    public DocumentCollectionResponse<FileEntryData> updateDocumentName(
            String collectionId, String documentId, UpdateDocumentRequest request, String userId) {

        // Find collection
        DocumentCollection collection = documentCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new NotFoundException("Collection not found with ID: " + collectionId));

        // Verify ownership
        if (!collection.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to update documents in this collection");
        }

        // Find the file entry
        FileEntry fileEntry = collection.getFiles().stream()
                .filter(file -> file.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Document not found with ID: " + documentId + " in collection: " + collectionId));

        // Update display name
        String oldDisplayName = fileEntry.getDisplayName();
        fileEntry.setDisplayName(request.getDisplayName());
        documentCollectionRepository.save(collection);

        log.info("Updated document {} display name from '{}' to '{}' in collection {} for user {}",
                documentId, oldDisplayName, request.getDisplayName(), collectionId, userId);

        // Build response
        FileEntryData fileEntryData = mapToFileEntryData(fileEntry);

        return DocumentCollectionResponse.<FileEntryData>builder()
                .statusCode(HttpStatus.OK.value())
                .status("success")
                .message("Document name updated successfully")
                .data(fileEntryData)
                .build();
    }

    private FileEntryData mapToFileEntryData(FileEntry fileEntry) {
        return FileEntryData.builder()
                .documentId(fileEntry.getDocumentId())
                .originalFileName(fileEntry.getOriginalFileName())
                .displayName(fileEntry.getDisplayName())
                .fileSize(fileEntry.getFileSize())
                .fileUrl(fileEntry.getFileUrl())
                .status(fileEntry.getUploadStatus())
                .isEncrypted(fileEntry.isEncrypted())
                .build();
    }
}
