package com.extractor.unraveldocs.payment.receipt.dto;

import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for passing receipt data between services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptData {
    private String userId;
    private String customerName;
    private String customerEmail;
    private PaymentProvider paymentProvider;
    private String externalPaymentId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentMethodDetails;
    private String description;
    private OffsetDateTime paidAt;
}
