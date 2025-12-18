package com.extractor.unraveldocs.payment.common.service;

import com.extractor.unraveldocs.payment.common.dto.*;
import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.user.model.User;

/**
 * Common interface for payment gateway operations.
 * This abstraction allows for unified handling of different payment providers
 * including Stripe, Paystack, Chappa, Flutterwave, and PayPal.
 */
public interface PaymentGatewayService {

    /**
     * Get the payment gateway provider this service implements.
     *
     * @return The payment gateway enum value
     */
    PaymentGateway getProvider();

    /**
     * Initialize a subscription payment for the user.
     *
     * @param user    The user subscribing to a plan
     * @param plan    The subscription plan
     * @param request Payment initialization request with gateway-specific options
     * @return Response containing payment URL and reference
     */
    InitializePaymentResponse initializeSubscriptionPayment(
            User user,
            SubscriptionPlan plan,
            InitializePaymentRequest request
    );

    /**
     * Create a one-time payment.
     *
     * @param user    The user making the payment
     * @param request Payment request details
     * @return Payment response with status and provider details
     */
    PaymentResponse createPayment(User user, PaymentRequest request);

    /**
     * Get payment details by provider payment ID.
     *
     * @param providerPaymentId The provider-specific payment ID
     * @return Payment response with current status
     */
    PaymentResponse getPayment(String providerPaymentId);

    /**
     * Process a refund for a payment.
     *
     * @param request Refund request details
     * @return Refund response with status
     */
    RefundResponse refundPayment(RefundRequest request);

    /**
     * Create a subscription for a user.
     *
     * @param user    The user subscribing
     * @param request Subscription request details
     * @return Subscription response with status
     */
    SubscriptionResponse createSubscription(User user, SubscriptionRequest request);

    /**
     * Get subscription details by provider subscription ID.
     *
     * @param providerSubscriptionId The provider-specific subscription ID
     * @return Subscription response with current status
     */
    SubscriptionResponse getSubscription(String providerSubscriptionId);

    /**
     * Cancel a subscription.
     *
     * @param providerSubscriptionId The provider-specific subscription ID
     * @param immediately Whether to cancel immediately or at period end
     * @return Subscription response with updated status
     */
    SubscriptionResponse cancelSubscription(String providerSubscriptionId, boolean immediately);

    /**
     * Change subscription plan (upgrade/downgrade).
     *
     * @param providerSubscriptionId The provider-specific subscription ID
     * @param newPriceId The new price/plan ID
     * @return Subscription response with updated plan
     */
    SubscriptionResponse changePlan(String providerSubscriptionId, String newPriceId);

    // ==================== Customers ====================

    /**
     * Get or create a customer for the user.
     *
     * @param user The user
     * @return Customer response with details
     */
    CustomerResponse getOrCreateCustomer(User user);

    /**
     * Verify a payment transaction.
     *
     * @param reference The payment reference to verify
     * @return true if payment was successful, false otherwise
     */
    boolean verifyPayment(String reference);

    /**
     * Cancel a subscription by ID.
     *
     * @param subscriptionId The subscription identifier
     * @return true if cancellation was successful
     * @deprecated Use {@link #cancelSubscription(String, boolean)} instead
     */
    @Deprecated
    default boolean cancelSubscription(String subscriptionId) {
        SubscriptionResponse response = cancelSubscription(subscriptionId, false);
        return response.isSuccess();
    }

    /**
     * Ensure a plan exists on the payment gateway and return its external ID.
     * Creates the plan if it doesn't exist.
     *
     * @param plan The subscription plan to sync
     * @return The payment gateway's plan code/ID
     */
    String ensurePlanExists(SubscriptionPlan plan);
}
