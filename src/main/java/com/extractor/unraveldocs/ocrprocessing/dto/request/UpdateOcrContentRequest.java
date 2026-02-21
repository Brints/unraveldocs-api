package com.extractor.unraveldocs.ocrprocessing.dto.request;

import com.extractor.unraveldocs.ocrprocessing.datamodel.ContentFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOcrContentRequest {
    @NotBlank(message = "Edited content must not be blank.")
    private String editedContent;

    @NotNull(message = "Content format is required.")
    private ContentFormat contentFormat;
}
