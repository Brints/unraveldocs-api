package com.extractor.unraveldocs.elasticsearch.controller;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.elasticsearch.document.PaymentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.document.UserSearchIndex;
import com.extractor.unraveldocs.elasticsearch.dto.SearchRequest;
import com.extractor.unraveldocs.elasticsearch.dto.SearchResponse;
import com.extractor.unraveldocs.elasticsearch.service.PaymentSearchService;
import com.extractor.unraveldocs.elasticsearch.service.UserSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for admin search operations.
 * Provides search endpoints for users and payments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
@Tag(name = "Admin Search", description = "Admin search endpoints for users and payments")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class AdminSearchController {

    private final UserSearchService userSearchService;
    private final PaymentSearchService paymentSearchService;
    private final SanitizeLogging sanitizer;

    // ==================== User Search ====================

    /**
     * Search users with advanced filtering.
     */
    @PostMapping("/users")
    @Operation(summary = "Search users", description = "Search users with filters and pagination")
    public ResponseEntity<SearchResponse<UserSearchIndex>> searchUsers(
            @Valid @RequestBody SearchRequest request) {

        log.debug("Admin user search: query={}", sanitizer.sanitizeLogging(request.getQuery()));

        SearchResponse<UserSearchIndex> response = userSearchService.searchUsers(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Quick user search with query parameter.
     */
    @GetMapping("/users")
    @Operation(summary = "Quick user search", description = "Simple user search with query parameter")
    public ResponseEntity<SearchResponse<UserSearchIndex>> quickUserSearch(
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by role") @RequestParam(required = false) String role,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Filter by country") @RequestParam(required = false) String country,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDirection) {

        SearchRequest.SearchRequestBuilder requestBuilder = SearchRequest.builder()
                .query(query)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection);

        if (role != null) {
            requestBuilder.filters(java.util.Map.of("role", role));
        } else if (isActive != null) {
            requestBuilder.filters(java.util.Map.of("isActive", isActive));
        } else if (country != null) {
            requestBuilder.filters(java.util.Map.of("country", country));
        }

        SearchResponse<UserSearchIndex> response = userSearchService.searchUsers(requestBuilder.build());
        return ResponseEntity.ok(response);
    }

    // ==================== Payment Search ====================

    /**
     * Search payments with advanced filtering.
     */
    @PostMapping("/payments")
    @Operation(summary = "Search payments", description = "Search payments with filters and pagination")
    public ResponseEntity<SearchResponse<PaymentSearchIndex>> searchPayments(
            @Valid @RequestBody SearchRequest request) {

        log.debug("Admin payment search: query={}", sanitizer.sanitizeLogging(request.getQuery()));

        SearchResponse<PaymentSearchIndex> response = paymentSearchService.searchPayments(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Quick payment search with query parameter.
     */
    @GetMapping("/payments")
    @Operation(summary = "Quick payment search", description = "Simple payment search with query parameter")
    public ResponseEntity<SearchResponse<PaymentSearchIndex>> quickPaymentSearch(
            @Parameter(description = "Search query (receipt number, email, name)") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by payment provider") @RequestParam(required = false) String paymentProvider,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by currency") @RequestParam(required = false) String currency,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDirection) {

        SearchRequest.SearchRequestBuilder requestBuilder = SearchRequest.builder()
                .query(query)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection);

        if (paymentProvider != null) {
            requestBuilder.filters(java.util.Map.of("paymentProvider", paymentProvider));
        } else if (status != null) {
            requestBuilder.filters(java.util.Map.of("status", status));
        } else if (currency != null) {
            requestBuilder.filters(java.util.Map.of("currency", currency));
        }

        SearchResponse<PaymentSearchIndex> response = paymentSearchService.searchPayments(requestBuilder.build());
        return ResponseEntity.ok(response);
    }

    /**
     * Find payment by receipt number.
     */
    @GetMapping("/payments/receipt/{receiptNumber}")
    @Operation(summary = "Find by receipt number", description = "Find payment by receipt number")
    public ResponseEntity<SearchResponse<PaymentSearchIndex>> findByReceiptNumber(
            @PathVariable String receiptNumber) {

        log.debug("Looking up receipt: {}", sanitizer.sanitizeLogging(receiptNumber));

        var page = paymentSearchService.findByReceiptNumber(
                receiptNumber,
                org.springframework.data.domain.PageRequest.of(0, 1));

        return ResponseEntity.ok(SearchResponse.fromPage(page));
    }
}
