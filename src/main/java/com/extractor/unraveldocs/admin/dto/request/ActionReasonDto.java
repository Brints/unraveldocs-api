package com.extractor.unraveldocs.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionReasonDto {
    @NotBlank(message = "Reason cannot be empty")
    private String reason;
}
