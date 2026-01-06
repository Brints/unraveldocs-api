package com.extractor.unraveldocs.payment.paypal.controller;

import com.extractor.unraveldocs.payment.paypal.dto.request.CreateOrderRequest;
import com.extractor.unraveldocs.payment.paypal.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.paypal.dto.request.RefundOrderRequest;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalCaptureResponse;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalOrderResponse;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalRefundResponse;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalSubscriptionResponse;
import com.extractor.unraveldocs.payment.paypal.model.PayPalPayment;
import com.extractor.unraveldocs.payment.paypal.model.PayPalSubscription;
import com.extractor.unraveldocs.payment.paypal.service.PayPalPaymentService;
import com.extractor.unraveldocs.payment.paypal.service.PayPalSubscriptionService;
import com.extractor.unraveldocs.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for PayPal payment operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/paypal")
@RequiredArgsConstructor
@Tag(name = "PayPal Payment", description = "Endpoints for PayPal payment operations")
public class PayPalPaymentController {

    private final PayPalPaymentService paymentService;
    private final PayPalSubscriptionService subscriptionService;

    // ==================== ORDER ENDPOINTS ====================

    @PostMapping("/orders")
    @Operation(summary = "Create a PayPal order", description = "Create a new order for one-time payment")
    public ResponseEntity<Map<String, Object>> createOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateOrderRequest request) {

        PayPalOrderResponse order = paymentService.createOrder(user, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", true,
                "message", "Order created successfully",
                "data", Map.of(
                        "orderId", order.getId(),
                        "status", order.getStatus(),
                        "approvalUrl", order.getApprovalLink() != null ? order.getApprovalLink() : "",
                        "links", order.getLinks() != null ? order.getLinks() : java.util.List.of())));
    }

    @PostMapping("/orders/{orderId}/capture")
    @Operation(summary = "Capture a PayPal order", description = "Capture payment for an approved order")
    public ResponseEntity<Map<String, Object>> captureOrder(@PathVariable String orderId) {

        PayPalCaptureResponse capture = paymentService.captureOrder(orderId);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", capture.isSuccessful() ? "Payment captured successfully" : "Capture pending",
                "data", Map.of(
                        "captureId", capture.getId() != null ? capture.getId() : "",
                        "orderId", capture.getOrderId(),
                        "status", capture.getStatus(),
                        "amount", capture.getAmount() != null ? capture.getAmount() : "",
                        "currency", capture.getCurrency() != null ? capture.getCurrency() : "")));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order details", description = "Retrieve PayPal order details")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable String orderId) {

        PayPalOrderResponse order = paymentService.getOrderDetails(orderId);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Order retrieved successfully",
                "data", order));
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund a payment", description = "Process a refund for a captured payment")
    public ResponseEntity<Map<String, Object>> refundPayment(@Valid @RequestBody RefundOrderRequest request) {

        PayPalRefundResponse refund = paymentService.refundPayment(request);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", refund.isSuccessful() ? "Refund processed successfully" : "Refund pending",
                "data", Map.of(
                        "refundId", refund.getId(),
                        "captureId", refund.getCaptureId(),
                        "status", refund.getStatus(),
                        "amount", refund.getAmount() != null ? refund.getAmount() : "",
                        "currency", refund.getCurrency() != null ? refund.getCurrency() : "")));
    }

    @GetMapping("/payments/history")
    @Operation(summary = "Get payment history", description = "Get paginated payment history for the authenticated user")
    public ResponseEntity<Page<PayPalPayment>> getPaymentHistory(
            @AuthenticationPrincipal User user,
            Pageable pageable) {

        Page<PayPalPayment> payments = paymentService.getUserPayments(user.getId(), pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/payments/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Get a specific payment by its order ID")
    public ResponseEntity<PayPalPayment> getPaymentByOrderId(@PathVariable String orderId) {
        return paymentService.getPaymentByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== SUBSCRIPTION ENDPOINTS ====================

    @PostMapping("/subscriptions")
    @Operation(summary = "Create a subscription", description = "Create a new PayPal subscription")
    public ResponseEntity<Map<String, Object>> createSubscription(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSubscriptionRequest request) {

        PayPalSubscriptionResponse subscription = subscriptionService.createSubscription(user, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", true,
                "message", "Subscription created successfully",
                "data", Map.of(
                        "subscriptionId", subscription.getId(),
                        "status", subscription.getStatus(),
                        "approvalUrl", subscription.getApprovalLink() != null ? subscription.getApprovalLink() : "")));
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Get subscription details", description = "Get PayPal subscription details")
    public ResponseEntity<Map<String, Object>> getSubscriptionDetails(@PathVariable String subscriptionId) {

        PayPalSubscriptionResponse subscription = subscriptionService.getSubscriptionDetails(subscriptionId);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Subscription retrieved successfully",
                "data", subscription));
    }

    @GetMapping("/subscriptions/active")
    @Operation(summary = "Get active subscription", description = "Get the active subscription for the authenticated user")
    public ResponseEntity<PayPalSubscription> getActiveSubscription(@AuthenticationPrincipal User user) {
        return subscriptionService.getActiveSubscriptionByUserId(user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Get subscription history", description = "Get paginated subscription history")
    public ResponseEntity<Page<PayPalSubscription>> getSubscriptionHistory(
            @AuthenticationPrincipal User user,
            Pageable pageable) {

        Page<PayPalSubscription> subscriptions = subscriptionService.getSubscriptionsByUserId(user.getId(), pageable);
        return ResponseEntity.ok(subscriptions);
    }

    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    @Operation(summary = "Cancel subscription", description = "Cancel a PayPal subscription")
    public ResponseEntity<Map<String, Object>> cancelSubscription(
            @PathVariable String subscriptionId,
            @RequestParam(required = false) String reason) {

        PayPalSubscriptionResponse subscription = subscriptionService.cancelSubscription(subscriptionId, reason);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Subscription cancelled successfully",
                "data", Map.of(
                        "subscriptionId", subscription.getId(),
                        "status", subscription.getStatus())));
    }

    @PostMapping("/subscriptions/{subscriptionId}/suspend")
    @Operation(summary = "Suspend subscription", description = "Suspend a PayPal subscription")
    public ResponseEntity<Map<String, Object>> suspendSubscription(
            @PathVariable String subscriptionId,
            @RequestParam(required = false) String reason) {

        PayPalSubscriptionResponse subscription = subscriptionService.suspendSubscription(subscriptionId, reason);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Subscription suspended successfully",
                "data", Map.of(
                        "subscriptionId", subscription.getId(),
                        "status", subscription.getStatus())));
    }

    @PostMapping("/subscriptions/{subscriptionId}/activate")
    @Operation(summary = "Activate subscription", description = "Activate/resume a suspended PayPal subscription")
    public ResponseEntity<Map<String, Object>> activateSubscription(
            @PathVariable String subscriptionId,
            @RequestParam(required = false) String reason) {

        PayPalSubscriptionResponse subscription = subscriptionService.activateSubscription(subscriptionId, reason);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Subscription activated successfully",
                "data", Map.of(
                        "subscriptionId", subscription.getId(),
                        "status", subscription.getStatus())));
    }

    // ==================== CALLBACK ENDPOINTS ====================

    @GetMapping("/return")
    @Operation(summary = "Payment return callback", description = "Handle return after PayPal approval")
    public ResponseEntity<Map<String, Object>> handleReturn(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String PayerID) {

        log.info("PayPal return callback - token: {}, PayerID: {}", token, PayerID);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Return callback received",
                "data", Map.of(
                        "token", token != null ? token : "",
                        "payerId", PayerID != null ? PayerID : "")));
    }

    @GetMapping("/cancel")
    @Operation(summary = "Payment cancel callback", description = "Handle cancellation from PayPal")
    public ResponseEntity<Map<String, Object>> handleCancel(@RequestParam(required = false) String token) {

        log.info("PayPal cancel callback - token: {}", token);

        return ResponseEntity.ok(Map.of(
                "status", false,
                "message", "Payment cancelled by user",
                "data", Map.of(
                        "token", token != null ? token : "")));
    }
}
