package com.extractor.unraveldocs.payment.paypal.dto.response;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.paypal.model.PayPalPayment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for PayPal payment history response to avoid circular references
 * and prevent leaking sensitive user data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalPaymentHistoryResponse {

    private String id;

    private String userId;

    private String userEmail;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("capture_id")
    private String captureId;

    @JsonProperty("authorization_id")
    private String authorizationId;

    @JsonProperty("subscription_id")
    private String subscriptionId;

    @JsonProperty("payment_type")
    private PaymentType paymentType;

    private PaymentStatus status;

    private BigDecimal amount;

    private String currency;

    @JsonProperty("amount_refunded")
    private BigDecimal amountRefunded;

    @JsonProperty("paypal_fee")
    private BigDecimal paypalFee;

    @JsonProperty("net_amount")
    private BigDecimal netAmount;

    private String intent;

    @JsonProperty("payer_id")
    private String payerId;

    @JsonProperty("payer_email")
    private String payerEmail;

    private String description;

    @JsonProperty("failure_message")
    private String failureMessage;

    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    /**
     * Convert PayPalPayment entity to PayPalPaymentHistoryResponse DTO.
     */
    public static PayPalPaymentHistoryResponse fromEntity(PayPalPayment payment) {
        if (payment == null) {
            return null;
        }

        return PayPalPaymentHistoryResponse.builder()
                .id(payment.getId())
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .userEmail(payment.getUser() != null ? payment.getUser().getEmail() : null)
                .orderId(payment.getOrderId())
                .captureId(payment.getCaptureId())
                .authorizationId(payment.getAuthorizationId())
                .subscriptionId(payment.getSubscriptionId())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .amountRefunded(payment.getAmountRefunded())
                .paypalFee(payment.getPaypalFee())
                .netAmount(payment.getNetAmount())
                .intent(payment.getIntent())
                .payerId(payment.getPayerId())
                .payerEmail(payment.getPayerEmail())
                .description(payment.getDescription())
                .failureMessage(payment.getFailureMessage())
                .completedAt(payment.getCompletedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
