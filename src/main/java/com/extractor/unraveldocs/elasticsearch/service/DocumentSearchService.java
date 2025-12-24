package com.extractor.unraveldocs.elasticsearch.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.elasticsearch.document.DocumentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.dto.DocumentSearchResult;
import com.extractor.unraveldocs.elasticsearch.dto.SearchRequest;
import com.extractor.unraveldocs.elasticsearch.dto.SearchResponse;
import com.extractor.unraveldocs.elasticsearch.events.ElasticsearchIndexEvent;
import com.extractor.unraveldocs.elasticsearch.events.IndexType;
import com.extractor.unraveldocs.elasticsearch.publisher.ElasticsearchEventPublisher;
import com.extractor.unraveldocs.elasticsearch.repository.DocumentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for document search operations using Elasticsearch.
 * Provides full-text search across OCR content and document metadata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class DocumentSearchService {

    private final DocumentSearchRepository documentSearchRepository;
    private final ElasticsearchEventPublisher eventPublisher;
    private final SanitizeLogging sanitizer;

    private static final int TEXT_PREVIEW_LENGTH = 200;

    /**
     * Searches documents for a specific user.
     *
     * @param userId  The user ID
     * @param request The search request
     * @return Search response with matching documents
     */
    public SearchResponse<DocumentSearchResult> searchDocuments(String userId, SearchRequest request) {
        log.debug("Searching documents for user {}: query='{}'", sanitizer.sanitizeLogging(userId), sanitizer.sanitizeLogging(request.getQuery()));

        Pageable pageable = createPageable(request);
        Page<DocumentSearchIndex> page;

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            page = documentSearchRepository.searchDocuments(userId, request.getQuery(), pageable);
        } else {
            page = documentSearchRepository.findByUserId(userId, pageable);
        }

        List<DocumentSearchResult> results = page.getContent().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());

        return SearchResponse.<DocumentSearchResult>builder()
                .results(results)
                .totalHits(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    /**
     * Searches documents by extracted text content.
     *
     * @param userId The user ID
     * @param query  The search query
     * @param page   Page number
     * @param size   Page size
     * @return Search response with matching documents
     */
    public SearchResponse<DocumentSearchResult> searchByContent(String userId, String query, int page, int size) {
        log.debug("Searching document content for user {}: query='{}'", sanitizer.sanitizeLogging(userId), sanitizer.sanitizeLogging(query));

        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentSearchIndex> results = documentSearchRepository
                .searchByUserIdAndExtractedText(userId, query, pageable);

        List<DocumentSearchResult> searchResults = results.getContent().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());

        return SearchResponse.<DocumentSearchResult>builder()
                .results(searchResults)
                .totalHits(results.getTotalElements())
                .page(results.getNumber())
                .size(results.getSize())
                .totalPages(results.getTotalPages())
                .build();
    }

    /**
     * Gets all documents for a user.
     *
     * @param userId   The user ID
     * @param pageable Pagination parameters
     * @return Page of documents
     */
    public Page<DocumentSearchIndex> getDocumentsByUserId(String userId, Pageable pageable) {
        return documentSearchRepository.findByUserId(userId, pageable);
    }

    /**
     * Indexes a document for search.
     *
     * @param document The document to index
     */
    public void indexDocument(DocumentSearchIndex document) {
        log.debug("Indexing document: {}", sanitizer.sanitizeLogging(document.getId()));

        String payload = eventPublisher.toJsonPayload(document);
        ElasticsearchIndexEvent event = ElasticsearchIndexEvent.createEvent(
                document.getId(),
                IndexType.DOCUMENT,
                payload);
        eventPublisher.publishDocumentIndexEvent(event);
    }

    /**
     * Indexes a document synchronously (for bulk operations).
     *
     * @param document The document to index
     */
    public void indexDocumentSync(DocumentSearchIndex document) {
        log.debug("Indexing document synchronously: {}", sanitizer.sanitizeLogging(document.getId()));
        documentSearchRepository.save(document);
    }

    /**
     * Deletes a document from the search index.
     *
     * @param documentId The document ID to delete
     */
    public void deleteDocument(String documentId) {
        log.debug("Deleting document from index: {}", sanitizer.sanitizeLogging(documentId));

        ElasticsearchIndexEvent event = ElasticsearchIndexEvent.deleteEvent(
                documentId,
                IndexType.DOCUMENT);
        eventPublisher.publishDocumentIndexEvent(event);
    }

    /**
     * Deletes all documents for a user from the search index.
     *
     * @param userId The user ID
     */
    public void deleteDocumentsByUserId(String userId) {
        log.info("Deleting all documents for user: {}", sanitizer.sanitizeLogging(userId));
        documentSearchRepository.deleteByUserId(userId);
    }

    /**
     * Deletes all documents in a collection from the search index.
     *
     * @param collectionId The collection ID
     */
    public void deleteDocumentsByCollectionId(String collectionId) {
        log.info("Deleting all documents for collection: {}", sanitizer.sanitizeLogging(collectionId));
        documentSearchRepository.deleteByCollectionId(collectionId);
    }

    private Pageable createPageable(SearchRequest request) {
        Sort sort = Sort.by(
                request.getSortDirection().equalsIgnoreCase("asc")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC,
                request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    private DocumentSearchResult toSearchResult(DocumentSearchIndex doc) {
        String textPreview = null;
        if (doc.getExtractedText() != null) {
            textPreview = doc.getExtractedText().length() > TEXT_PREVIEW_LENGTH
                    ? doc.getExtractedText().substring(0, TEXT_PREVIEW_LENGTH) + "..."
                    : doc.getExtractedText();
        }

        return DocumentSearchResult.builder()
                .id(doc.getId())
                .collectionId(doc.getCollectionId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .ocrStatus(doc.getOcrStatus())
                .textPreview(textPreview)
                .fileUrl(doc.getFileUrl())
                .uploadTimestamp(doc.getUploadTimestamp())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
