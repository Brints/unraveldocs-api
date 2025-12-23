package com.extractor.unraveldocs.elasticsearch.controller;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.elasticsearch.dto.DocumentSearchResult;
import com.extractor.unraveldocs.elasticsearch.dto.SearchRequest;
import com.extractor.unraveldocs.elasticsearch.dto.SearchResponse;
import com.extractor.unraveldocs.elasticsearch.service.DocumentSearchService;
import com.extractor.unraveldocs.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for document search operations.
 * Allows users to search their documents using full-text search.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/search/documents")
@RequiredArgsConstructor
@Tag(name = "Document Search", description = "Full-text search for documents")
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class DocumentSearchController {

        private final DocumentSearchService documentSearchService;
        private final SanitizeLogging sanitize;

        /**
         * Search documents with full-text query.
         */
        @PostMapping
        @Operation(summary = "Search documents", description = "Search user's documents using full-text search")
        public ResponseEntity<SearchResponse<DocumentSearchResult>> searchDocuments(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody SearchRequest request) {

                log.debug("Document search request from user {}: {}", sanitize.sanitizeLogging(user.getId()),
                                sanitize.sanitizeLogging(request.getQuery()));

                SearchResponse<DocumentSearchResult> response = documentSearchService
                                .searchDocuments(user.getId(), request);

                return ResponseEntity.ok(response);
        }

        /**
         * Search documents by content (OCR extracted text).
         */
        @GetMapping("/content")
        @Operation(summary = "Search document content", description = "Search within OCR-extracted text")
        public ResponseEntity<SearchResponse<DocumentSearchResult>> searchByContent(
                        @AuthenticationPrincipal User user,
                        @Parameter(description = "Search query") @RequestParam String query,
                        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

                log.debug("Content search request from user {}: query={}", sanitize.sanitizeLogging(user.getId()),
                                sanitize.sanitizeLogging(query));

                SearchResponse<DocumentSearchResult> response = documentSearchService
                                .searchByContent(user.getId(), query, page, size);

                return ResponseEntity.ok(response);
        }

        /**
         * Quick search with simple query parameter.
         */
        @GetMapping
        @Operation(summary = "Quick document search", description = "Simple search with query parameter")
        public ResponseEntity<SearchResponse<DocumentSearchResult>> quickSearch(
                        @AuthenticationPrincipal User user,
                        @Parameter(description = "Search query") @RequestParam(required = false) String query,
                        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
                        @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
                        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDirection) {

                SearchRequest request = SearchRequest.builder()
                                .query(query)
                                .page(page)
                                .size(size)
                                .sortBy(sortBy)
                                .sortDirection(sortDirection)
                                .build();

                SearchResponse<DocumentSearchResult> response = documentSearchService
                                .searchDocuments(user.getId(), request);

                return ResponseEntity.ok(response);
        }
}
