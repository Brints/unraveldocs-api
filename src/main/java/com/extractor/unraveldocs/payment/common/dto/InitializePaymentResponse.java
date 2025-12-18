package com.extractor.unraveldocs.payment.common.dto;

import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common response DTO for payment initialization across different payment gateways.
 * Contains all necessary information to complete the payment flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializePaymentResponse {

    private PaymentGateway gateway;

    /**
     * The URL to redirect the user to for payment completion.
     * For Stripe: Checkout session URL
     * For Paystack: Authorization URL
     */
    private String paymentUrl;

    /**
     * Unique reference for this payment transaction.
     */
    private String reference;

    /**
     * Gateway-specific session or access code.
     * For Stripe: Session ID
     * For Paystack: Access Code
     */
    private String accessCode;

    /**
     * Indicates if the initialization was successful.
     */
    private boolean success;

    /**
     * Error message if initialization failed.
     */
    private String errorMessage;
}
