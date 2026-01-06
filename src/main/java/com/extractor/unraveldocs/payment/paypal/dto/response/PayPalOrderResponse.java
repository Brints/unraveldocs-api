package com.extractor.unraveldocs.payment.paypal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for PayPal order operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalOrderResponse {

    private String id;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String approvalUrl;
    private String captureUrl;
    private List<LinkDescription> links;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;

    /**
     * Payer information from PayPal.
     */
    private PayerInfo payer;

    /**
     * Purchase unit details.
     */
    private List<PurchaseUnit> purchaseUnits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkDescription {
        private String href;
        private String rel;
        private String method;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayerInfo {
        private String payerId;
        private String email;
        private String firstName;
        private String lastName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseUnit {
        private String referenceId;
        private AmountBreakdown amount;
        private String description;
        private String customId;
        private List<CaptureInfo> captures;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmountBreakdown {
        private String currencyCode;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaptureInfo {
        private String id;
        private String status;
        private AmountBreakdown amount;
        private OffsetDateTime createTime;
    }

    /**
     * Extract the approval URL from links.
     */
    public String getApprovalLink() {
        if (links == null)
            return null;
        return links.stream()
                .filter(link -> "approve".equals(link.getRel()))
                .map(LinkDescription::getHref)
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if the order is approved and ready for capture.
     */
    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    /**
     * Check if the order payment is completed.
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
}
