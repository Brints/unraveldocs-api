package com.extractor.unraveldocs.documents.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCollectionRequest {
    @NotBlank(message = "Collection name is required")
    @Size(max = 255, message = "Collection name cannot exceed 255 characters")
    private String name;
}
