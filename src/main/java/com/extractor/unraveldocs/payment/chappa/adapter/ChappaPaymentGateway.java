package com.extractor.unraveldocs.payment.chappa.adapter;

import com.extractor.unraveldocs.payment.common.dto.*;
import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.common.service.PaymentGatewayService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of PaymentGatewayService for Chappa.
 * This is a placeholder for future implementation of Chappa payment integration.
 * 
 * Chappa is a payment gateway primarily serving Ethiopia.
 */
@Slf4j
@Service
public class ChappaPaymentGateway implements PaymentGatewayService {

    private static final String NOT_IMPLEMENTED_MSG = "Chappa payment gateway is not yet implemented";

    @Override
    public PaymentGateway getProvider() {
        return PaymentGateway.CHAPA;
    }

    @Override
    public InitializePaymentResponse initializeSubscriptionPayment(User user, SubscriptionPlan plan, InitializePaymentRequest request) {
        log.warn(NOT_IMPLEMENTED_MSG);
        return InitializePaymentResponse.builder()
                .gateway(PaymentGateway.CHAPA)
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
