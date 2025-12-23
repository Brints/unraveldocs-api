package com.extractor.unraveldocs.elasticsearch.controller;

import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Elasticsearch synchronization operations.
 * Admin-only endpoints for managing search indexes.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/elasticsearch")
@RequiredArgsConstructor
@Tag(name = "Elasticsearch Sync", description = "Admin endpoints for Elasticsearch synchronization")
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class ElasticsearchSyncController {

    private final ElasticsearchSyncService syncService;

    /**
     * Triggers a full synchronization of all data to Elasticsearch.
     * This is an async operation that runs in the background.
     */
    @PostMapping("/sync")
    @Operation(summary = "Full sync", description = "Synchronize all data from PostgreSQL to Elasticsearch")
    public ResponseEntity<Map<String, String>> syncAll() {
        log.info("Full Elasticsearch sync triggered");

        syncService.syncAll();

        return ResponseEntity.accepted().body(Map.of(
                "message", "Full synchronization started in background",
                "status", "STARTED"));
    }

    /**
     * Synchronizes only users to Elasticsearch.
     */
    @PostMapping("/sync/users")
    @Operation(summary = "Sync users", description = "Synchronize all users to Elasticsearch")
    public ResponseEntity<Map<String, Object>> syncUsers() {
        log.info("User sync triggered");

        int count = syncService.syncAllUsers();

        return ResponseEntity.ok(Map.of(
                "message", "User synchronization completed",
                "usersIndexed", count));
    }

    /**
     * Synchronizes only documents to Elasticsearch.
     */
    @PostMapping("/sync/documents")
    @Operation(summary = "Sync documents", description = "Synchronize all documents to Elasticsearch")
    public ResponseEntity<Map<String, Object>> syncDocuments() {
        log.info("Document sync triggered");

        int count = syncService.syncAllDocuments();

        return ResponseEntity.ok(Map.of(
                "message", "Document synchronization completed",
                "documentsIndexed", count));
    }

    /**
     * Synchronizes only payments to Elasticsearch.
     */
    @PostMapping("/sync/payments")
    @Operation(summary = "Sync payments", description = "Synchronize all payments to Elasticsearch")
    public ResponseEntity<Map<String, Object>> syncPayments() {
        log.info("Payment sync triggered");

        int count = syncService.syncAllPayments();

        return ResponseEntity.ok(Map.of(
                "message", "Payment synchronization completed",
                "paymentsIndexed", count));
    }
}
