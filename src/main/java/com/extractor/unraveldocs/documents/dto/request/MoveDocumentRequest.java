package com.extractor.unraveldocs.documents.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveDocumentRequest {
    @NotBlank(message = "Source collection ID is required")
    private String sourceCollectionId;

    @NotBlank(message = "Target collection ID is required")
    private String targetCollectionId;

    @NotBlank(message = "Document ID is required")
    private String documentId;
}
