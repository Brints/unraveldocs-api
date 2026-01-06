package com.extractor.unraveldocs.storage.controller;

import com.extractor.unraveldocs.security.CurrentUser;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.storage.dto.StorageInfo;
import com.extractor.unraveldocs.storage.service.StorageAllocationService;
import com.extractor.unraveldocs.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for storage-related operations.
 */
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Storage management endpoints")
public class StorageController {

    private final StorageAllocationService storageAllocationService;

    /**
     * Get current storage usage and limits for the authenticated user.
     */
    @GetMapping
    @Operation(summary = "Get storage info", description = "Returns current storage usage and limits for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Storage info retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated")
    })
    public ResponseEntity<UnravelDocsResponse<StorageInfo>> getStorageInfo(@CurrentUser User user) {
        StorageInfo storageInfo = storageAllocationService.getStorageInfo(user);

        UnravelDocsResponse<StorageInfo> response = new UnravelDocsResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setStatus("success");
        response.setMessage("Storage information retrieved successfully");
        response.setData(storageInfo);

        return ResponseEntity.ok(response);
    }
}
