package com.extractor.unraveldocs.payment.paystack.dto.response;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.paystack.model.PaystackPayment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for payment history response to avoid circular references
 * and prevent leaking sensitive user data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {

    private String id;

    private String userId;

    private String userEmail;

    @JsonProperty("transaction_id")
    private Long transactionId;

    private String reference;

    @JsonProperty("plan_code")
    private String planCode;

    @JsonProperty("subscription_code")
    private String subscriptionCode;

    @JsonProperty("payment_type")
    private PaymentType paymentType;

    private PaymentStatus status;

    private BigDecimal amount;

    private String currency;

    @JsonProperty("amount_refunded")
    private BigDecimal amountRefunded;

    private BigDecimal fees;

    private String channel;

    @JsonProperty("gateway_response")
    private String gatewayResponse;

    private String description;

    @JsonProperty("failure_message")
    private String failureMessage;

    @JsonProperty("paid_at")
    private OffsetDateTime paidAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    /**
     * Convert PaystackPayment entity to PaymentHistoryResponse DTO
     */
    public static PaymentHistoryResponse fromEntity(PaystackPayment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentHistoryResponse.builder()
                .id(payment.getId())
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .userEmail(payment.getUser() != null ? payment.getUser().getEmail() : null)
                .transactionId(payment.getTransactionId())
                .reference(payment.getReference())
                .planCode(payment.getPlanCode())
                .subscriptionCode(payment.getSubscriptionCode())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .amountRefunded(payment.getAmountRefunded())
                .fees(payment.getFees())
                .channel(payment.getChannel())
                .gatewayResponse(payment.getGatewayResponse())
                .description(payment.getDescription())
                .failureMessage(payment.getFailureMessage())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
