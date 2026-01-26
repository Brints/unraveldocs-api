package com.extractor.unraveldocs.coupon.service;

import com.extractor.unraveldocs.coupon.dto.request.BulkCouponGenerationRequest;
import com.extractor.unraveldocs.coupon.dto.response.BulkGenerationJobResponse;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;

/**
 * Service interface for bulk coupon generation via Kafka.
 */
public interface BulkCouponGenerationService {

    /**
     * Initiates bulk coupon generation. Returns job ID for tracking.
     * Publishes to Kafka topic for async processing.
     */
    UnravelDocsResponse<BulkGenerationJobResponse> generateBulkCoupons(
            BulkCouponGenerationRequest request,
            User createdBy);

    /**
     * Gets the status of a bulk generation job.
     */
    UnravelDocsResponse<BulkGenerationJobResponse> getJobStatus(String jobId);

    /**
     * Cancels a pending or in-progress bulk generation job.
     */
    UnravelDocsResponse<Void> cancelJob(String jobId, User user);
}
