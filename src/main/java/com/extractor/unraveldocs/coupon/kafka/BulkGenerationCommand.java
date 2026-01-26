package com.extractor.unraveldocs.coupon.kafka;

import com.extractor.unraveldocs.coupon.dto.request.BulkCouponGenerationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Kafka event for bulk coupon generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkGenerationCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String jobId;
    private String userId;
    private BulkCouponGenerationRequest request;
    private OffsetDateTime timestamp;
    private int retryCount;
}
