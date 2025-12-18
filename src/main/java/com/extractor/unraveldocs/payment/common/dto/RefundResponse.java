package com.extractor.unraveldocs.payment.common.dto;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Unified response DTO for refund operations across all payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private PaymentGateway provider;

    private String id;

    private String providerRefundId;

    private String paymentId;

    private String status;

    private BigDecimal amount;

    private String currency;

    private String reason;

    private OffsetDateTime createdAt;

    private boolean success;

    private String errorMessage;
}
