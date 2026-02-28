package com.extractor.unraveldocs.payment.paypal.adapter;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.common.dto.*;
import com.extractor.unraveldocs.payment.common.enums.PaymentGateway;
import com.extractor.unraveldocs.payment.common.service.PaymentGatewayService;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.SubscriptionStatus;
import com.extractor.unraveldocs.payment.paypal.config.PayPalConfig;
import com.extractor.unraveldocs.payment.paypal.dto.request.CreateOrderRequest;
import com.extractor.unraveldocs.payment.paypal.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.paypal.dto.request.RefundOrderRequest;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalOrderResponse;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalRefundResponse;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalSubscriptionResponse;
import com.extractor.unraveldocs.payment.paypal.model.PayPalCustomer;
import com.extractor.unraveldocs.payment.paypal.model.PayPalPayment;
import com.extractor.unraveldocs.payment.paypal.service.PayPalCustomerService;
import com.extractor.unraveldocs.payment.paypal.service.PayPalPaymentService;
import com.extractor.unraveldocs.payment.paypal.service.PayPalSubscriptionService;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Implementation of PaymentGatewayService for PayPal.
 * Provides a unified interface for PayPal payment operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalPaymentGateway implements PaymentGatewayService {

    private final PayPalPaymentService paymentService;
    private final PayPalCustomerService customerService;
    private final PayPalSubscriptionService subscriptionService;
    private final PayPalConfig payPalConfig;
    private final SanitizeLogging sanitizer;

    @Override
    public PaymentGateway getProvider() {
        return PaymentGateway.PAYPAL;
    }

    @Override
    public InitializePaymentResponse initializeSubscriptionPayment(User user, SubscriptionPlan plan,
            InitializePaymentRequest request) {
        try {
            log.info("Initializing PayPal subscription payment for user: {}, plan: {}",
                    sanitizer.sanitizeLogging(user.getId()),
                    sanitizer.sanitizeLoggingObject(plan.getName()));

            CreateSubscriptionRequest subRequest = CreateSubscriptionRequest.builder()
                    .planId(plan.getPaypalPlanCode())
                    .returnUrl(payPalConfig.getReturnUrl() != null && !payPalConfig.getReturnUrl().isBlank()
                            ? UriComponentsBuilder.fromUriString(payPalConfig.getReturnUrl())
                                    .queryParam("redirect_url", request.getCallbackUrl()).build().toUriString()
                            : request.getCallbackUrl())
                    .cancelUrl(payPalConfig.getCancelUrl() != null && !payPalConfig.getCancelUrl().isBlank()
                            ? UriComponentsBuilder.fromUriString(payPalConfig.getCancelUrl())
                                    .queryParam("redirect_url", request.getCancelUrl()).build().toUriString()
                            : request.getCancelUrl())
                    .customId(user.getId())
                    .build();

            PayPalSubscriptionResponse subscription = subscriptionService.createSubscription(user, subRequest);

            return InitializePaymentResponse.builder()
                    .gateway(PaymentGateway.PAYPAL)
                    .success(true)
                    .paymentUrl(subscription.getApprovalLink())
                    .reference(subscription.getId())
                    .accessCode(subscription.getId())
                    .build();

        } catch (Exception e) {
            log.error(
                    "Failed to initialize PayPal subscription payment: {}",
                    sanitizer.sanitizeLogging(e.getMessage()), e);
            return InitializePaymentResponse.builder()
                    .gateway(PaymentGateway.PAYPAL)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponse createPayment(User user, PaymentRequest request) {
        try {
            log.info(
                    "Creating PayPal payment for user: {}, amount: {}",
                    sanitizer.sanitizeLogging(user.getId()),
                    sanitizer.sanitizeLoggingInteger(Math.toIntExact(request.getAmount())));

            // Convert cents to dollars
            BigDecimal amountInDollars = BigDecimal.valueOf(request.getAmount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                    .amount(amountInDollars)
                    .currency(request.getCurrency())
                    .description(request.getDescription())
                    .metadata(request.getMetadata())
                    .returnUrl(payPalConfig.getReturnUrl() != null && !payPalConfig.getReturnUrl().isBlank()
                            ? UriComponentsBuilder.fromUriString(payPalConfig.getReturnUrl())
                                    .queryParam("redirect_url", request.getCallbackUrl()).build().toUriString()
                            : request.getCallbackUrl())
                    .cancelUrl(payPalConfig.getCancelUrl() != null && !payPalConfig.getCancelUrl().isBlank()
                            ? UriComponentsBuilder.fromUriString(payPalConfig.getCancelUrl())
                                    .queryParam("redirect_url", request.getCancelUrl()).build().toUriString()
                            : request.getCancelUrl())
                    .build();

            PayPalOrderResponse order = paymentService.createOrder(user, orderRequest);

            return PaymentResponse.builder()
                    .success(true)
                    .providerPaymentId(order.getId())
                    .status(mapOrderStatus(order.getStatus()))
                    .amount(order.getAmount())
                    .currency(order.getCurrency())
                    .paymentUrl(order.getApprovalLink())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create PayPal payment: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponse getPayment(String providerPaymentId) {
        try {
            PayPalOrderResponse order = paymentService.getOrderDetails(providerPaymentId);

            return PaymentResponse.builder()
                    .success(true)
                    .providerPaymentId(order.getId())
                    .status(mapOrderStatus(order.getStatus()))
                    .amount(order.getAmount())
                    .currency(order.getCurrency())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get PayPal payment: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public RefundResponse refundPayment(RefundRequest request) {
        try {
            log.info("Processing PayPal refund for payment: {}", sanitizer.sanitizeLogging(request.getPaymentId()));

            // Get the capture ID from the payment
            Optional<PayPalPayment> paymentOpt = paymentService.getPaymentByOrderId(request.getPaymentId());
            if (paymentOpt.isEmpty()) {
                paymentOpt = paymentService.getPaymentByCaptureId(request.getPaymentId());
            }

            if (paymentOpt.isEmpty()) {
                return RefundResponse.builder()
                        .success(false)
                        .errorMessage("Payment not found")
                        .build();
            }

            PayPalPayment payment = paymentOpt.get();
            String captureId = payment.getCaptureId() != null ? payment.getCaptureId() : request.getPaymentId();

            // Convert cents to dollars if amount is provided
            BigDecimal refundAmount = null;
            if (request.getAmount() != null) {
                refundAmount = BigDecimal.valueOf(request.getAmount())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }

            RefundOrderRequest refundRequest = RefundOrderRequest.builder()
                    .captureId(captureId)
                    .amount(refundAmount)
                    .reason(request.getReason())
                    .build();

            PayPalRefundResponse refund = paymentService.refundPayment(refundRequest);

            return RefundResponse.builder()
                    .success(refund.isSuccessful())
                    .providerRefundId(refund.getId())
                    .amount(refund.getAmount())
                    .status(refund.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("Failed to process PayPal refund: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            return RefundResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse createSubscription(User user, SubscriptionRequest request) {
        try {
            log.info(
                    "Creating PayPal subscription for user: {}",
                    sanitizer.sanitizeLogging(user.getId()));

            CreateSubscriptionRequest subRequest = CreateSubscriptionRequest.builder()
                    .planId(request.getPriceId())
                    .returnUrl(request.getSuccessUrl())
                    .cancelUrl(request.getCancelUrl())
                    .build();

            PayPalSubscriptionResponse subscription = subscriptionService.createSubscription(user, subRequest);

            return SubscriptionResponse.builder()
                    .success(true)
                    .providerSubscriptionId(subscription.getId())
                    .status(mapSubscriptionStatus(subscription.getStatus()))
                    .build();

        } catch (Exception e) {
            log.error(
                    "Failed to create PayPal subscription: {}",
                    sanitizer.sanitizeLogging(e.getMessage()), e);
            return SubscriptionResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse getSubscription(String providerSubscriptionId) {
        try {
            PayPalSubscriptionResponse subscription = subscriptionService
                    .getSubscriptionDetails(providerSubscriptionId);

            return SubscriptionResponse.builder()
                    .success(true)
                    .providerSubscriptionId(subscription.getId())
                    .status(mapSubscriptionStatus(subscription.getStatus()))
                    .currentPeriodStart(subscription.getStartTime())
                    .currentPeriodEnd(subscription.getBillingInfo() != null
                            ? subscription.getBillingInfo().getNextBillingTime()
                            : null)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get PayPal subscription: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            return SubscriptionResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse cancelSubscription(String providerSubscriptionId, boolean immediately) {
        try {
            log.info(
                    "Cancelling PayPal subscription: {}",
                    sanitizer.sanitizeLogging(providerSubscriptionId));

            PayPalSubscriptionResponse subscription = subscriptionService.cancelSubscription(
                    providerSubscriptionId,
                    immediately ? "Immediate cancellation requested" : "Cancellation at period end");

            return SubscriptionResponse.builder()
                    .success(true)
                    .providerSubscriptionId(subscription.getId())
                    .status(mapSubscriptionStatus(subscription.getStatus()))
                    .canceledAt(java.time.OffsetDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to cancel PayPal subscription: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            return SubscriptionResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SubscriptionResponse changePlan(String providerSubscriptionId, String newPriceId) {
        // PayPal subscription plan changes require creating a new subscription
        // This is a limitation of PayPal's subscription API
        log.warn("PayPal subscription plan change not directly supported. " +
                "Cancel existing and create new subscription.");

        return SubscriptionResponse.builder()
                .success(false)
                .errorMessage("Plan change requires cancelling and creating a new subscription")
                .build();
    }

    @Override
    public CustomerResponse getOrCreateCustomer(User user) {
        try {
            PayPalCustomer customer = customerService.getOrCreateCustomer(user);

            return CustomerResponse.builder()
                    .success(true)
                    .providerCustomerId(customer.getPayerId() != null ? customer.getPayerId() : customer.getId())
                    .email(customer.getEmail())
                    .name(customer.getFirstName() + " " + customer.getLastName())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get/create PayPal customer: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            return CustomerResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean verifyPayment(String reference) {
        try {
            PayPalOrderResponse order = paymentService.getOrderDetails(reference);
            return order.isCompleted();

        } catch (Exception e) {
            log.error("Failed to verify PayPal payment: {}", sanitizer.sanitizeLogging(e.getMessage()), e);
            return false;
        }
    }

    @Override
    public String ensurePlanExists(SubscriptionPlan plan) {
        // Return the existing plan code if set
        if (plan.getPaypalPlanCode() != null && !plan.getPaypalPlanCode().isEmpty()) {
            return plan.getPaypalPlanCode();
        }

        log.warn("PayPal plan code not set for subscription plan: {}. " +
                "Please create the plan in PayPal and update the subscription_plans table.",
                sanitizer.sanitizeLoggingObject(plan.getName()));
        return null;
    }

    // ==================== Helper Methods ====================

    private PaymentStatus mapOrderStatus(String paypalStatus) {
        if (paypalStatus == null)
            return PaymentStatus.PENDING;

        return switch (paypalStatus.toUpperCase()) {
            case "APPROVED" -> PaymentStatus.PROCESSING;
            case "COMPLETED" -> PaymentStatus.SUCCEEDED;
            case "VOIDED" -> PaymentStatus.CANCELED;
            default -> PaymentStatus.PENDING;
        };
    }

    private SubscriptionStatus mapSubscriptionStatus(String paypalStatus) {
        if (paypalStatus == null)
            return SubscriptionStatus.INCOMPLETE;

        return switch (paypalStatus.toUpperCase()) {
            case "APPROVED", "ACTIVE" -> SubscriptionStatus.ACTIVE;
            case "SUSPENDED" -> SubscriptionStatus.PAUSED;
            case "CANCELLED", "EXPIRED" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.INCOMPLETE;
        };
    }
}
