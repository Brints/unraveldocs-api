package com.extractor.unraveldocs.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for page count calculation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageCountData {
    private Integer totalPages;
    private Integer creditsRequired;
    private Integer currentBalance;
    private Boolean hasEnoughCredits;
}
