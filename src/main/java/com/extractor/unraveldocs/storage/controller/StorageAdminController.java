package com.extractor.unraveldocs.storage.controller;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.storage.service.StorageDataMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin controller for storage management operations.
 */
@RestController
@RequestMapping("/api/v1/admin/storage")
@RequiredArgsConstructor
@Tag(name = "Storage Admin", description = "Admin endpoints for storage management")
public class StorageAdminController {

    private final StorageDataMigrationService storageDataMigrationService;

    /**
     * Run storage data migration to calculate and update storage usage for all
     * users.
     * This is intended to be run once after deploying the storage allocation
     * feature.
     */
    @PostMapping("/migrate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Run storage migration", description = "Calculates and updates storage usage for all users based on their current documents. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Migration completed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN role")
    })
    public ResponseEntity<UnravelDocsResponse<StorageDataMigrationService.MigrationResult>> runMigration() {
        StorageDataMigrationService.MigrationResult result = storageDataMigrationService.migrateStorageData();

        UnravelDocsResponse<StorageDataMigrationService.MigrationResult> response = new UnravelDocsResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setStatus("success");
        response.setMessage(result.getSummary());
        response.setData(result);

        return ResponseEntity.ok(response);
    }
}
