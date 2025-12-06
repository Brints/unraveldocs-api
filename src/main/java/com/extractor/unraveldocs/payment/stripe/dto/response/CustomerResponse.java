package com.extractor.unraveldocs.payment.stripe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for customer information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    
    private String id;
    
    private String email;
    
    private String name;
    
    private String defaultPaymentMethodId;
    
    private List<PaymentMethod> paymentMethods;
    
    private Long createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethod {
        private String id;
        private String type;
        private Card card;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Card {
            private String brand;
            private String last4;
            private Long expMonth;
            private Long expYear;
        }
    }
}
