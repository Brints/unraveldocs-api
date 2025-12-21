package com.extractor.unraveldocs.payment.receipt.events;

import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Kafka event payload for receipt generation requests.
 * Published when a payment is successful and a receipt needs to be generated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptRequestedEvent implements Serializable {
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

