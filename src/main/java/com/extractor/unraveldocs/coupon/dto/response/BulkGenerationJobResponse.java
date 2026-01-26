package com.extractor.unraveldocs.coupon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for bulk coupon generation job status.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkGenerationJobResponse {
    private String jobId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private int totalRequested;
    private int successfullyCreated;
    private int failed;
    private List<String> createdCouponCodes;
    private List<String> errors;
    private String startedAt;
    private String completedAt;
    private int progressPercentage;
}
