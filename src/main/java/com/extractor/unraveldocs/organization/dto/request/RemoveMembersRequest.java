package com.extractor.unraveldocs.organization.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveMembersRequest {
    @NotEmpty(message = "At least one user ID is required")
    @Size(max = 10, message = "Cannot remove more than 10 members at once")
    private List<String> userIds;
}
