package com.extractor.unraveldocs.ocrprocessing.controller;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionUploadData;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.ocrprocessing.dto.response.CollectionResultResponse;
import com.extractor.unraveldocs.ocrprocessing.dto.response.FileResultData;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.service.OcrService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
@Tag(name = "OCR Document Processing", description = "Endpoints for processing documents using OCR")
public class OcrDocumentController {
        private final OcrService ocrService;
        private final UserRepository userRepository;

        private User getAuthenticatedUser(Authentication authenticatedUser) {
                if (authenticatedUser == null) {
                        throw new ForbiddenException("You must be logged in to perform this action.");
                }
                String email = authenticatedUser.getName();
                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new ForbiddenException("User not found. Please log in again."));
        }

        @Operation(summary = "Extract text from a specific file", description = "Triggers the OCR process to extract text from a file. If already processed, returns existing data.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully extracted text or retrieved existing data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OcrData.class))),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized or not logged in"),
                        @ApiResponse(responseCode = "404", description = "Not Found - Collection or file not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error - OCR processing failed")
        })
        @PostMapping("/{collectionId}/document/{documentId}/extract")
        public ResponseEntity<DocumentCollectionResponse<OcrData>> extractTextFromFile(
                        @Parameter(description = "ID of the document collection", required = true) @PathVariable String collectionId,

                        @Parameter(description = "ID of the document to extract text from", required = true) @PathVariable String documentId,

                        @Parameter(description = "Start page for PDF extraction (1-indexed, inclusive). Only applies to PDF files.") @RequestParam(required = false) Integer startPage,

                        @Parameter(description = "End page for PDF extraction (1-indexed, inclusive). Only applies to PDF files.") @RequestParam(required = false) Integer endPage,

                        @Parameter(description = "Specific pages to extract from PDF (1-indexed, comma-separated). Takes priority over startPage/endPage. Example: ?pages=3,8,16") @RequestParam(required = false) java.util.List<Integer> pages,

                        Authentication authenticatedUser) {
                User user = getAuthenticatedUser(authenticatedUser);
                OcrData ocrData = ocrService.extractTextFromDocument(collectionId, documentId, user.getId(),
                                startPage, endPage, pages);

                DocumentCollectionResponse<OcrData> response = DocumentCollectionResponse.<OcrData>builder()
                                .statusCode(HttpStatus.OK.value())
                                .status("success")
                                .message("Text extraction completed successfully.")
                                .data(ocrData)
                                .build();

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Extract text from all files in a collection", description = "Triggers the OCR process to extract text from all files in a collection.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully extracted text from all files", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OcrData.class))),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized or not logged in"),
                        @ApiResponse(responseCode = "404", description = "Not Found - Collection not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error - OCR processing failed")
        })
        @PostMapping(value = "/upload/extract-all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<DocumentCollectionResponse<DocumentCollectionUploadData>> extractTextFromAllFiles(
                        @Parameter(description = "Files to be uploaded and extracted", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) @RequestParam("files") @NotNull MultipartFile[] files,
                        Authentication authenticatedUser) {
                User user = getAuthenticatedUser(authenticatedUser);

                if (files == null || files.length == 0) {
                        throw new BadRequestException("No files provided for extraction.");
                }

                DocumentCollectionResponse<DocumentCollectionUploadData> response = ocrService.uploadDocuments(files,
                                user);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get OCR results for a document collection", description = "Retrieves the OCR results for a specific document collection.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved OCR results", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CollectionResultResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized or not logged in"),
                        @ApiResponse(responseCode = "404", description = "Not Found - Collection not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to retrieve results")
        })
        @GetMapping("/{collectionId}/document/results")
        public ResponseEntity<DocumentCollectionResponse<CollectionResultResponse>> getOcrResults(
                        @Parameter(description = "ID of the document collection", required = true) @PathVariable String collectionId) {
                DocumentCollectionResponse<CollectionResultResponse> response = ocrService
                                .getCollectionResult(collectionId);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get OCR data for a specific document", description = "Retrieves the OCR data for a specific document in a collection.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved OCR data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OcrData.class))),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized or not logged in"),
                        @ApiResponse(responseCode = "404", description = "Not Found - Document or OCR data not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to retrieve OCR data")
        })
        @GetMapping("/{collectionId}/document/{documentId}/ocr-data")
        public ResponseEntity<DocumentCollectionResponse<FileResultData>> getOcrData(
                        @Parameter(description = "ID of the document collection", required = true) @PathVariable String collectionId,
                        @Parameter(description = "ID of the document to retrieve OCR data for", required = true) @PathVariable String documentId,
                        Authentication authenticatedUser) {
                User user = getAuthenticatedUser(authenticatedUser);
                DocumentCollectionResponse<FileResultData> response = ocrService.getOcrData(collectionId, documentId,
                                user.getId());

                return ResponseEntity.ok(response);
        }
}
