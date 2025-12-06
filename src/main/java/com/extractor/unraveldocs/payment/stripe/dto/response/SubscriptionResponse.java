package com.extractor.unraveldocs.payment.stripe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for subscription operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    
    private String id;
    
    private String status;
    
    private String customerId;
    
    private Long currentPeriodStart;
    
    private Long currentPeriodEnd;
    
    private Long trialEnd;
    
    private String defaultPaymentMethodId;
    
    private String latestInvoiceId;
    
    private List<SubscriptionItem> items;
    
    private boolean cancelAtPeriodEnd;
    
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
