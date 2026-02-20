package com.extractor.unraveldocs.ai.controller;

import com.extractor.unraveldocs.ai.dto.request.SummarizeRequest;
import com.extractor.unraveldocs.ai.dto.response.ClassifyResponse;
import com.extractor.unraveldocs.ai.dto.response.SummarizeResponse;
import com.extractor.unraveldocs.ai.service.AiClassificationService;
import com.extractor.unraveldocs.ai.service.AiSummarizationService;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI-powered document operations.
 * Provides endpoints for document summarization and classification.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Operations", description = "Endpoints for AI-powered document analysis")
public class AiController {

    private final AiSummarizationService summarizationService;
    private final AiClassificationService classificationService;
    private final ResponseBuilderService responseBuilder;
    private final UserRepository userRepository;

    @Operation(summary = "Summarize document text", description = "Generate an AI summary of OCR-extracted text. "
            + "Uses subscription allowance first, then credits as overflow.", responses = {
                    @ApiResponse(responseCode = "200", description = "Summary generated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UnravelDocsResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - OCR not complete or insufficient quota"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User not authenticated"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Document or OCR data not found")
            })
    @PostMapping("/summarize")
    public ResponseEntity<UnravelDocsResponse<SummarizeResponse>> summarize(
            @Valid @RequestBody SummarizeRequest request,
            Authentication authenticatedUser) {

        User user = getAuthenticatedUser(authenticatedUser);
        SummarizeResponse result = summarizationService.summarize(user.getId(), request);

        UnravelDocsResponse<SummarizeResponse> response = responseBuilder.buildUserResponse(
                result, HttpStatus.OK, "Document summarized successfully.");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Classify document", description = "AI-powered document classification and tagging. "
            + "Identifies document type and generates descriptive tags from OCR text.", responses = {
                    @ApiResponse(responseCode = "200", description = "Document classified successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UnravelDocsResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - OCR not complete or insufficient quota"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User not authenticated"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Document or OCR data not found")
            })
    @PostMapping("/classify/{documentId}")
    public ResponseEntity<UnravelDocsResponse<ClassifyResponse>> classify(
            @Parameter(description = "ID of the document to classify", required = true) @PathVariable String documentId,
            Authentication authenticatedUser) {

        User user = getAuthenticatedUser(authenticatedUser);
        ClassifyResponse result = classificationService.classify(user.getId(), documentId);

        UnravelDocsResponse<ClassifyResponse> response = responseBuilder.buildUserResponse(
                result, HttpStatus.OK, "Document classified successfully.");

        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser(Authentication authenticatedUser) {
        if (authenticatedUser == null) {
            throw new ForbiddenException("You must be logged in to perform this action.");
        }
        String email = authenticatedUser.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("User not found. Please log in again."));
    }
}
