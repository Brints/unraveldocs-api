package com.extractor.unraveldocs.payment.receipt.dto;

import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for receipt API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponseDto {
    private String id;
    private String receiptNumber;
    private PaymentProvider paymentProvider;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentMethodDetails;
    private String description;
    private String receiptUrl;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
}
