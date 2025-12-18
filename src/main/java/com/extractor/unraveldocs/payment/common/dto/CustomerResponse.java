package com.extractor.unraveldocs.payment.common.dto;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Unified response DTO for customer operations across all payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private PaymentGateway provider;

    /**
     * Internal customer ID
     */
    private String id;

    /**
     * Provider-specific customer ID
     */
    private String providerCustomerId;

    private String email;

    private String name;

    private String phone;

    /**
     * Default payment method ID
     */
    private String defaultPaymentMethodId;

    /**
     * List of saved payment methods
     */
    private List<PaymentMethodInfo> paymentMethods;

    private OffsetDateTime createdAt;

    private boolean success;

    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodInfo {
        private String id;
        private String type;
        private String brand;
        private String last4;
        private Integer expMonth;
        private Integer expYear;
        private boolean isDefault;
    }
}
