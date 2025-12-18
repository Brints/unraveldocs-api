package com.extractor.unraveldocs.payment.common.dto;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Unified response DTO for subscription operations across all providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private PaymentGateway provider;

    /**
     * Internal subscription ID
     */
    private String id;

    /**
     * Provider-specific subscription ID
     */
    private String providerSubscriptionId;

    private SubscriptionStatus status;

    private String customerId;

    private String priceId;

    private String planName;

    private Long quantity;

    private String currency;

    private OffsetDateTime currentPeriodStart;

    private OffsetDateTime currentPeriodEnd;

    private OffsetDateTime trialEnd;

    private boolean cancelAtPeriodEnd;

    private OffsetDateTime canceledAt;

    private String defaultPaymentMethodId;

    private String latestInvoiceId;

    /**
     * Subscription items (for multi-item subscriptions)
     */
    private List<SubscriptionItem> items;

    private boolean success;

    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionItem {
        private String id;
        private String priceId;
        private Long quantity;
        private Long unitAmount;
        private String currency;
    }
}
