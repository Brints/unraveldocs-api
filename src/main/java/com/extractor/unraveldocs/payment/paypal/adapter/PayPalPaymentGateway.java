package com.extractor.unraveldocs.payment.paypal.adapter;

import com.extractor.unraveldocs.payment.common.dto.*;
import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.common.service.PaymentGatewayService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of PaymentGatewayService for PayPal.
 * This is a placeholder for future implementation of PayPal payment integration.
 * 
 * PayPal is a global payment gateway with worldwide coverage.
 */
@Slf4j
@Service
public class PayPalPaymentGateway implements PaymentGatewayService {

    private static final String NOT_IMPLEMENTED_MSG = "PayPal payment gateway is not yet implemented";

    @Override
    public PaymentGateway getProvider() {
        return PaymentGateway.PAYPAL;
    }

    @Override
    public InitializePaymentResponse initializeSubscriptionPayment(User user, SubscriptionPlan plan, InitializePaymentRequest request) {
        log.warn(NOT_IMPLEMENTED_MSG);
        return InitializePaymentResponse.builder()
                .gateway(PaymentGateway.PAYPAL)
                .success(false)
                .errorMessage(NOT_IMPLEMENTED_MSG)
                .build();
    }

    @Override
    public PaymentResponse createPayment(User user, PaymentRequest request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public PaymentResponse getPayment(String providerPaymentId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public RefundResponse refundPayment(RefundRequest request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public SubscriptionResponse createSubscription(User user, SubscriptionRequest request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public SubscriptionResponse getSubscription(String providerSubscriptionId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public SubscriptionResponse cancelSubscription(String providerSubscriptionId, boolean immediately) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public SubscriptionResponse changePlan(String providerSubscriptionId, String newPriceId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public CustomerResponse getOrCreateCustomer(User user) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public boolean verifyPayment(String reference) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public String ensurePlanExists(SubscriptionPlan plan) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }
}
