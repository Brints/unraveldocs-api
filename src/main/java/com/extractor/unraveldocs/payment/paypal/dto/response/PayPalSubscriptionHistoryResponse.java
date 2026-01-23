package com.extractor.unraveldocs.payment.paypal.dto.response;

import com.extractor.unraveldocs.payment.paypal.model.PayPalSubscription;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for PayPal subscription history response to avoid circular references
 * and prevent leaking sensitive user data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalSubscriptionHistoryResponse {

    private String id;

    private String userId;

    private String userEmail;

    @JsonProperty("subscription_id")
    private String subscriptionId;

    @JsonProperty("plan_id")
    private String planId;

    private String status;

    private BigDecimal amount;

    private String currency;

    @JsonProperty("custom_id")
    private String customId;

    @JsonProperty("start_time")
    private OffsetDateTime startTime;

    @JsonProperty("next_billing_time")
    private OffsetDateTime nextBillingTime;

    @JsonProperty("outstanding_balance")
    private BigDecimal outstandingBalance;

    @JsonProperty("cycles_completed")
    private Integer cyclesCompleted;

    @JsonProperty("failed_payments_count")
    private Integer failedPaymentsCount;

    @JsonProperty("last_payment_time")
    private OffsetDateTime lastPaymentTime;

    @JsonProperty("last_payment_amount")
    private BigDecimal lastPaymentAmount;

    @JsonProperty("auto_renewal")
    private Boolean autoRenewal;

    @JsonProperty("cancelled_at")
    private OffsetDateTime cancelledAt;

    @JsonProperty("status_change_reason")
    private String statusChangeReason;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    /**
     * Convert PayPalSubscription entity to PayPalSubscriptionHistoryResponse DTO.
     */
    public static PayPalSubscriptionHistoryResponse fromEntity(PayPalSubscription subscription) {
        if (subscription == null) {
            return null;
        }

        return PayPalSubscriptionHistoryResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser() != null ? subscription.getUser().getId() : null)
                .userEmail(subscription.getUser() != null ? subscription.getUser().getEmail() : null)
                .subscriptionId(subscription.getSubscriptionId())
                .planId(subscription.getPlanId())
                .status(subscription.getStatus())
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .customId(subscription.getCustomId())
                .startTime(subscription.getStartTime())
                .nextBillingTime(subscription.getNextBillingTime())
                .outstandingBalance(subscription.getOutstandingBalance())
                .cyclesCompleted(subscription.getCyclesCompleted())
                .failedPaymentsCount(subscription.getFailedPaymentsCount())
                .lastPaymentTime(subscription.getLastPaymentTime())
                .lastPaymentAmount(subscription.getLastPaymentAmount())
                .autoRenewal(subscription.getAutoRenewal())
                .cancelledAt(subscription.getCancelledAt())
                .statusChangeReason(subscription.getStatusChangeReason())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
