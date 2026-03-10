package com.extractor.unraveldocs.wordexport.controller;

import com.extractor.unraveldocs.security.CurrentUser;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.wordexport.interfaces.DownloadOcrResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collections/{collectionId}/documents/{documentId}/download")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Download OCR Result", description = "Controller for downloading OCR results as DOCX files")
public class DownloadOcrCResultController {
    private final DownloadOcrResultService downloadOcrResultService;

    /**
     * Generates and streams a DOCX file containing the extracted text of a specific document.
     * The response headers are set to trigger a file download dialog in the browser.
     *
     * @param collectionId The ID of the document's collection.
     * @param documentId   The ID of the document whose OCR text is to be downloaded.
     * @param user         The currently authenticated user, injected by Spring Security.
     * @return A {@link ResponseEntity} containing the {@link InputStreamResource} of the DOCX file.
     */
    @GetMapping(value = "/docx", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    @Operation(summary = "Download extracted Text as DOCX file",
               description = "Generates and downloads the OCR result of a document in DOCX format.")
    public ResponseEntity<InputStreamResource> downloadOcrResultAsDocx(
            @Parameter(description = "ID of the document collection", required = true) @PathVariable String collectionId,
            @Parameter(description = "ID of the document to export text", required = true) @PathVariable String documentId,
            @Parameter(description = "Type of text to download (original or edited)", example = "original") @RequestParam(defaultValue = "original", required = false) String type,
            @Parameter(hidden = true) @CurrentUser User user
            ) {
        DownloadOcrResultService.DownloadableFile downloadableFile = downloadOcrResultService
                .downloadAsDocx(collectionId, documentId, type, user.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadableFile.fileName() + "\"");

        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(downloadableFile.resource());
    }
}
