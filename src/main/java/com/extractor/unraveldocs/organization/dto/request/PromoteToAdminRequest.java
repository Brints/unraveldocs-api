package com.extractor.unraveldocs.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoteToAdminRequest {
    @NotBlank(message = "User ID is required")
    private String userId;
}
