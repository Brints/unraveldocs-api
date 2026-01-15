package com.extractor.unraveldocs.documents.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCollectionRequest {
    @Size(max = 255, message = "Collection name cannot exceed 255 characters")
    private String collectionName;  // Optional, auto-generated if not provided

    private boolean enableEncryption;  // Premium feature - requires Starter+ subscription
}
