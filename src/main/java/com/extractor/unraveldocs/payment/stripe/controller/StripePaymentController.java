package com.extractor.unraveldocs.payment.stripe.controller;

import com.extractor.unraveldocs.payment.stripe.dto.request.CreatePaymentIntentRequest;
import com.extractor.unraveldocs.payment.stripe.dto.request.RefundRequest;
import com.extractor.unraveldocs.payment.stripe.dto.response.PaymentIntentResponse;
import com.extractor.unraveldocs.payment.stripe.dto.response.RefundResponse;
import com.extractor.unraveldocs.payment.stripe.model.StripePayment;
import com.extractor.unraveldocs.payment.stripe.service.StripePaymentService;
import com.extractor.unraveldocs.payment.stripe.service.StripeService;
import com.extractor.unraveldocs.user.model.User;
import com.stripe.exception.StripeException;
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

/**
 * Controller for one-time payment operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stripe/payment")
@RequiredArgsConstructor
@Tag(name = "Stripe Payment", description = "Endpoints for one-time payments")
public class StripePaymentController {

    private final StripeService stripeService;
    private final StripePaymentService paymentService;

    @PostMapping("/create-payment-intent")
    @Operation(summary = "Create a payment intent for one-time payment")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        try {
            PaymentIntentResponse response = stripeService.createPaymentIntent(user, request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to create payment intent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/intent/{paymentIntentId}")
    @Operation(summary = "Get payment intent status")
    public ResponseEntity<PaymentIntentResponse> getPaymentIntent(@PathVariable String paymentIntentId) {
        try {
            PaymentIntentResponse response = stripeService.getPaymentIntent(paymentIntentId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to retrieve payment intent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "Process a refund")
    public ResponseEntity<RefundResponse> processRefund(@Valid @RequestBody RefundRequest request) {
        try {
            RefundResponse response = stripeService.processRefund(request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to process refund: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/history")
    @Operation(summary = "Get payment history for current user")
    public ResponseEntity<Page<StripePayment>> getPaymentHistory(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        Page<StripePayment> payments = paymentService.getUserPayments(user.getId(), pageable);
        return ResponseEntity.ok(payments);
    }
}
