package com.extractor.unraveldocs.payment.paypal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for PayPal subscription operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalSubscriptionResponse {

    private String id;
    private String planId;
    private String status;
    private OffsetDateTime startTime;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;

    /**
     * Approval URL for the subscriber to approve the subscription.
     */
    private String approvalUrl;

    /**
     * Custom ID for tracking.
     */
    private String customId;

    /**
     * Billing info for the subscription.
     */
    private BillingInfo billingInfo;

    /**
     * Subscriber information.
     */
    private SubscriberInfo subscriber;

    /**
     * Links for subscription actions.
     */
    private List<PayPalOrderResponse.LinkDescription> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingInfo {
        private BigDecimal outstandingBalance;
        private String currency;
        private Integer cycleExecutionsCount;
        private Integer failedPaymentsCount;
        private OffsetDateTime lastPaymentTime;
        private BigDecimal lastPaymentAmount;
        private OffsetDateTime nextBillingTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriberInfo {
        private String payerId;
        private String email;
        private String firstName;
        private String lastName;
    }

    /**
     * Extract the approval URL from links.
     */
    public String getApprovalLink() {
        if (links == null)
            return null;
        return links.stream()
                .filter(link -> "approve".equals(link.getRel()))
                .map(PayPalOrderResponse.LinkDescription::getHref)
                .findFirst()
                .orElse(approvalUrl);
    }

    /**
     * Check if the subscription is active.
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * Check if the subscription is pending approval.
     */
    public boolean isPendingApproval() {
        return "APPROVAL_PENDING".equals(status);
    }

    /**
     * Check if the subscription is cancelled.
     */
    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    /**
     * Check if the subscription is suspended.
     */
    public boolean isSuspended() {
        return "SUSPENDED".equals(status);
    }
}
