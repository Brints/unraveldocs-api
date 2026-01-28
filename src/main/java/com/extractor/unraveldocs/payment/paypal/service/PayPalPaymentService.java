package com.extractor.unraveldocs.payment.paypal.service;

import com.extractor.unraveldocs.coupon.dto.request.ApplyCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.DiscountCalculationData;
import com.extractor.unraveldocs.coupon.exception.InvalidCouponException;
import com.extractor.unraveldocs.coupon.service.CouponValidationService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.paypal.config.PayPalConfig;
import com.extractor.unraveldocs.payment.paypal.dto.request.CreateOrderRequest;
import com.extractor.unraveldocs.payment.paypal.dto.request.RefundOrderRequest;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalCaptureResponse;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalOrderResponse;
import com.extractor.unraveldocs.payment.paypal.dto.response.PayPalRefundResponse;
import com.extractor.unraveldocs.payment.paypal.exception.PayPalPaymentException;
import com.extractor.unraveldocs.payment.paypal.model.PayPalCustomer;
import com.extractor.unraveldocs.payment.paypal.model.PayPalPayment;
import com.extractor.unraveldocs.payment.paypal.repository.PayPalPaymentRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for PayPal payment operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalPaymentService {

    private final PayPalConfig payPalConfig;
    private final PayPalAuthService authService;
    private final PayPalCustomerService customerService;
    private final PayPalPaymentRepository paymentRepository;
    private final CouponValidationService couponValidationService;
    private final RestClient paypalRestClient;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitizer;

    /**
     * Create a PayPal order for payment.
     */
    @Transactional
    public PayPalOrderResponse createOrder(User user, CreateOrderRequest request) {
        log.info("Creating PayPal order for user: {}, amount: {} {}",
                sanitizer.sanitizeLogging(user.getId()),
                sanitizer.sanitizeLoggingObject(request.getAmount()),
                sanitizer.sanitizeLogging(request.getCurrency()));

        try {
            // Ensure customer exists
            PayPalCustomer customer = customerService.getOrCreateCustomer(user);

            // Track coupon discount info
            BigDecimal originalAmount = request.getAmount();
            BigDecimal finalAmount = request.getAmount();
            BigDecimal discountAmount = BigDecimal.ZERO;
            String appliedCouponCode = null;

            // Validate and apply coupon if provided
            if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
                ApplyCouponRequest couponRequest = new ApplyCouponRequest();
                couponRequest.setCouponCode(request.getCouponCode());
                couponRequest.setAmount(originalAmount);

                DiscountCalculationData discountData = couponValidationService.applyCouponToAmount(couponRequest, user);

                // Check minimum purchase requirement
                if (discountData.getMinPurchaseAmount() != null
                        && !discountData.isMinPurchaseRequirementMet()) {
                    throw new InvalidCouponException(
                            String.format("Minimum purchase amount of %s required for this coupon",
                                    discountData.getMinPurchaseAmount()));
                }

                finalAmount = discountData.getFinalAmount();
                discountAmount = discountData.getDiscountAmount();
                appliedCouponCode = discountData.getCouponCode();

                log.info("Coupon applied: original={}, discount={}, final={}",
                        sanitizer.sanitizeLoggingObject(originalAmount),
                        sanitizer.sanitizeLoggingObject(discountAmount),
                        sanitizer.sanitizeLoggingObject(finalAmount));
            }

            // Build order request
            String currency = request.getCurrency() != null ? request.getCurrency() : payPalConfig.getDefaultCurrency();
            String returnUrl = request.getReturnUrl() != null ? request.getReturnUrl() : payPalConfig.getReturnUrl();
            String cancelUrl = request.getCancelUrl() != null ? request.getCancelUrl() : payPalConfig.getCancelUrl();

            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("intent", request.getIntent());
            orderRequest.put("purchase_units", List.of(Map.of(
                    "amount", Map.of(
                            "currency_code", currency,
                            "value", finalAmount.toString()),
                    "description", request.getDescription() != null ? request.getDescription() : "Payment")));
            orderRequest.put("application_context", Map.of(
                    "return_url", returnUrl,
                    "cancel_url", cancelUrl,
                    "brand_name", "UnravelDocs",
                    "landing_page", "LOGIN",
                    "user_action", "PAY_NOW"));

            String response = paypalRestClient.post()
                    .uri("/v2/checkout/orders")
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .body(orderRequest)
                    .retrieve()
                    .body(String.class);

            JsonNode orderJson = objectMapper.readTree(response);
            PayPalOrderResponse orderResponse = parseOrderResponse(orderJson);

            // Record the payment with coupon info
            recordPayment(user, customer, orderResponse, request,
                    originalAmount, finalAmount, discountAmount, appliedCouponCode);

            log.info("Created PayPal order: {}", sanitizer.sanitizeLogging(orderResponse.getId()));
            return orderResponse;

        } catch (InvalidCouponException e) {
            log.warn("Coupon validation failed for user {}: {}", sanitizer.sanitizeLogging(user.getId()), e.getMessage());
            throw e;
        } catch (PayPalPaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create PayPal order: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to create PayPal order", e);
        }
    }

    /**
     * Capture a PayPal order after buyer approval.
     */
    @Transactional
    public PayPalCaptureResponse captureOrder(String orderId) {
        log.info("Capturing PayPal order: {}", sanitizer.sanitizeLogging(orderId));

        try {
            String response = paypalRestClient.post()
                    .uri("/v2/checkout/orders/{orderId}/capture", orderId)
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .retrieve()
                    .body(String.class);

            JsonNode captureJson = objectMapper.readTree(response);
            PayPalCaptureResponse captureResponse = parseCaptureResponse(captureJson, orderId);

            // Update payment record
            updatePaymentAfterCapture(orderId, captureResponse);

            log.info("Captured PayPal order: {}, status: {}",
                    sanitizer.sanitizeLogging(orderId),
                    sanitizer.sanitizeLogging(captureResponse.getStatus()));
            return captureResponse;

        } catch (PayPalPaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to capture PayPal order: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to capture PayPal order", e);
        }
    }

    /**
     * Get order details from PayPal.
     */
    public PayPalOrderResponse getOrderDetails(String orderId) {
        log.debug("Getting PayPal order details: {}", sanitizer.sanitizeLogging(orderId));

        try {
            String response = paypalRestClient.get()
                    .uri("/v2/checkout/orders/{orderId}", orderId)
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .retrieve()
                    .body(String.class);

            JsonNode orderJson = objectMapper.readTree(response);
            return parseOrderResponse(orderJson);

        } catch (Exception e) {
            log.error("Failed to get PayPal order details: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to get order details", e);
        }
    }

    /**
     * Process a refund for a captured payment.
     */
    @Transactional
    public PayPalRefundResponse refundPayment(RefundOrderRequest request) {
        log.info("Refunding PayPal capture: {}", sanitizer.sanitizeLogging(request.getCaptureId()));

        try {
            Map<String, Object> refundRequest = buildRefundRequest(request);

            String response = paypalRestClient.post()
                    .uri("/v2/payments/captures/{captureId}/refund", request.getCaptureId())
                    .header(HttpHeaders.AUTHORIZATION, authService.getAuthorizationHeader())
                    .body(refundRequest)
                    .retrieve()
                    .body(String.class);

            JsonNode refundJson = objectMapper.readTree(response);
            PayPalRefundResponse refundResponse = parseRefundResponse(refundJson, request.getCaptureId());

            // Update payment record
            updatePaymentAfterRefund(request.getCaptureId(), refundResponse);

            log.info("Refunded PayPal capture: {}, status: {}",
                    sanitizer.sanitizeLogging(request.getCaptureId()),
                    sanitizer.sanitizeLogging(refundResponse.getStatus()));
            return refundResponse;

        } catch (PayPalPaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to refund PayPal payment: {}", e.getMessage(), e);
            throw new PayPalPaymentException("Failed to process refund", e);
        }
    }

    /**
     * Get payment by order ID.
     */
    public Optional<PayPalPayment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Get payment by capture ID.
     */
    public Optional<PayPalPayment> getPaymentByCaptureId(String captureId) {
        return paymentRepository.findByCaptureId(captureId);
    }

    /**
     * Get user's payment history.
     */
    public Page<PayPalPayment> getUserPayments(String userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get payments by status.
     */
    public Page<PayPalPayment> getPaymentsByStatus(String userId, PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    /**
     * Count successful payments for a user.
     */
    public long countSuccessfulPayments(String userId) {
        return paymentRepository.countByUserIdAndStatus(userId, PaymentStatus.SUCCEEDED);
    }

    /**
     * Update payment status.
     */
    @Transactional
    public void updatePaymentStatus(String orderId, PaymentStatus status, String failureMessage) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(status);
            if (failureMessage != null) {
                payment.setFailureMessage(failureMessage);
            }
            if (status == PaymentStatus.SUCCEEDED) {
                payment.setCompletedAt(OffsetDateTime.now());
            }
            paymentRepository.save(payment);
            log.info("Updated payment {} status to {}",
                    sanitizer.sanitizeLogging(payment.getId()),
                    sanitizer.sanitizeLoggingObject(status));
        });
    }

    // ==================== Private Helper Methods ====================

    private void recordPayment(User user, PayPalCustomer customer,
            PayPalOrderResponse orderResponse, CreateOrderRequest request,
            BigDecimal originalAmount, BigDecimal finalAmount, BigDecimal discountAmount, String couponCode) {
        PayPalPayment payment = PayPalPayment.builder()
                .user(user)
                .paypalCustomer(customer)
                .orderId(orderResponse.getId())
                .paymentType(PaymentType.ONE_TIME)
                .status(PaymentStatus.PENDING)
                .amount(finalAmount) // Store the discounted amount
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .couponCode(couponCode)
                .currency(request.getCurrency() != null ? request.getCurrency() : payPalConfig.getDefaultCurrency())
                .intent(request.getIntent())
                .description(request.getDescription())
                .build();

        if (request.getMetadata() != null) {
            try {
                payment.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize payment metadata: {}", e.getMessage());
            }
        }

        paymentRepository.save(payment);
    }

    private void updatePaymentAfterCapture(String orderId, PayPalCaptureResponse captureResponse) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setCaptureId(captureResponse.getId());
            payment.setStatus(captureResponse.isSuccessful() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);
            if (captureResponse.isSuccessful()) {
                payment.setCompletedAt(OffsetDateTime.now());
            }
            paymentRepository.save(payment);
        });
    }

    private void updatePaymentAfterRefund(String captureId, PayPalRefundResponse refundResponse) {
        paymentRepository.findByCaptureId(captureId).ifPresent(payment -> {
            BigDecimal currentRefunded = payment.getAmountRefunded() != null ? payment.getAmountRefunded()
                    : BigDecimal.ZERO;
            payment.setAmountRefunded(currentRefunded.add(refundResponse.getAmount()));

            if (payment.getAmountRefunded().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            paymentRepository.save(payment);
        });
    }

    private PayPalOrderResponse parseOrderResponse(JsonNode orderJson) {
        PayPalOrderResponse.PayPalOrderResponseBuilder builder = PayPalOrderResponse.builder()
                .id(orderJson.get("id").asText())
                .status(orderJson.get("status").asText());

        if (orderJson.has("links")) {
            List<PayPalOrderResponse.LinkDescription> links = new java.util.ArrayList<>();
            for (JsonNode linkNode : orderJson.get("links")) {
                links.add(PayPalOrderResponse.LinkDescription.builder()
                        .href(linkNode.get("href").asText())
                        .rel(linkNode.get("rel").asText())
                        .method(linkNode.has("method") ? linkNode.get("method").asText() : "GET")
                        .build());
            }
            builder.links(links);
        }

        if (orderJson.has("purchase_units") && !orderJson.get("purchase_units").isEmpty()) {
            JsonNode unit = orderJson.get("purchase_units").get(0);
            if (unit.has("amount")) {
                builder.amount(new BigDecimal(unit.get("amount").get("value").asText()));
                builder.currency(unit.get("amount").get("currency_code").asText());
            }
        }

        return builder.build();
    }

    private PayPalCaptureResponse parseCaptureResponse(JsonNode captureJson, String orderId) {
        PayPalCaptureResponse.PayPalCaptureResponseBuilder builder = PayPalCaptureResponse.builder()
                .orderId(orderId)
                .status(captureJson.get("status").asText());

        if (captureJson.has("purchase_units") && !captureJson.get("purchase_units").isEmpty()) {
            JsonNode unit = captureJson.get("purchase_units").get(0);
            if (unit.has("payments") && unit.get("payments").has("captures")) {
                JsonNode capture = unit.get("payments").get("captures").get(0);
                builder.id(capture.get("id").asText());
                if (capture.has("amount")) {
                    builder.amount(new BigDecimal(capture.get("amount").get("value").asText()));
                    builder.currency(capture.get("amount").get("currency_code").asText());
                }
            }
        }

        return builder.build();
    }

    private PayPalRefundResponse parseRefundResponse(JsonNode refundJson, String captureId) {
        return PayPalRefundResponse.builder()
                .id(refundJson.get("id").asText())
                .captureId(captureId)
                .status(refundJson.get("status").asText())
                .amount(refundJson.has("amount") ? new BigDecimal(refundJson.get("amount").get("value").asText())
                        : null)
                .currency(refundJson.has("amount") ? refundJson.get("amount").get("currency_code").asText() : null)
                .build();
    }

    private Map<String, Object> buildRefundRequest(RefundOrderRequest request) {
        if (request.getAmount() != null) {
            return Map.of(
                    "amount", Map.of(
                            "currency_code",
                            request.getCurrency() != null ? request.getCurrency() : payPalConfig.getDefaultCurrency(),
                            "value", request.getAmount().toString()),
                    "note_to_payer", request.getNoteToPayer() != null ? request.getNoteToPayer() : "Refund processed");
        }
        return Map.of("note_to_payer",
                request.getNoteToPayer() != null ? request.getNoteToPayer() : "Refund processed");
    }
}
