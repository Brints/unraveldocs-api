package com.extractor.unraveldocs.admin.dto.response;

import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Admin view of a receipt including basic user details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReceiptDto {
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
    private boolean emailSent;
    private OffsetDateTime createdAt;

    // User details
    private String userId;
    private String userEmail;
    private String userFullName;
}
