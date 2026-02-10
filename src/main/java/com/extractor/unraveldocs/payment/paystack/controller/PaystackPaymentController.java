package com.extractor.unraveldocs.payment.paystack.controller;

import com.extractor.unraveldocs.payment.paystack.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.paystack.dto.request.InitializeTransactionRequest;
import com.extractor.unraveldocs.payment.paystack.dto.response.InitializeTransactionData;
import com.extractor.unraveldocs.payment.paystack.dto.response.PaymentHistoryResponse;
import com.extractor.unraveldocs.payment.paystack.dto.response.TransactionData;
import com.extractor.unraveldocs.payment.paystack.model.PaystackPayment;
import com.extractor.unraveldocs.payment.paystack.model.PaystackSubscription;
import com.extractor.unraveldocs.payment.paystack.service.PaystackPaymentService;
import com.extractor.unraveldocs.payment.paystack.service.PaystackSubscriptionService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.subscription.service.UserSubscriptionService;
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
 * Controller for Paystack payment operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/paystack")
@RequiredArgsConstructor
@Tag(name = "Paystack Payment", description = "Endpoints for Paystack payment operations")
public class PaystackPaymentController {

        private final PaystackPaymentService paymentService;
        private final PaystackSubscriptionService subscriptionService;
        private final UserSubscriptionService userSubscriptionService;

        // ==================== TRANSACTION ENDPOINTS ====================

        @PostMapping("/transaction/initialize")
        @Operation(summary = "Initialize a transaction", description = "Initialize a one-time payment or subscription transaction")
        public ResponseEntity<UnravelDocsResponse<InitializeTransactionData>> initializeTransaction(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody InitializeTransactionRequest request) {

                // Validate subscription eligibility
                userSubscriptionService.validateSubscriptionEligibility(user);

                InitializeTransactionData data = paymentService.initializeTransaction(user, request);

                return ResponseEntity.ok(new UnravelDocsResponse<>(
                                HttpStatus.OK.value(),
                                "success",
                                "Transaction initialized successfully",
                                data));
        }

        @GetMapping("/transaction/verify/{reference}")
        @Operation(summary = "Verify a transaction", description = "Verify the status of a transaction by reference")
        public ResponseEntity<UnravelDocsResponse<TransactionData>> verifyTransaction(@PathVariable String reference) {
                TransactionData data = paymentService.verifyTransaction(reference);

                var response = new UnravelDocsResponse<>(
                        HttpStatus.OK.value(),
                        "success",
                        "Transaction verified successfully",
                        data);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/transaction/charge-authorization")
        @Operation(summary = "Charge an authorization", description = "Charge a previously authorized card for recurring payments")
        public ResponseEntity<UnravelDocsResponse<TransactionData>> chargeAuthorization(
                        @AuthenticationPrincipal User user,
                        @RequestParam String authorizationCode,
                        @RequestParam Long amount,
                        @RequestParam(required = false) String currency) {

                TransactionData data = paymentService.chargeAuthorization(user, authorizationCode, amount, currency);

                var response = new UnravelDocsResponse<>(
                        HttpStatus.OK.value(),
                        "success",
                        "Authorization charged successfully",
                        data);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/transaction/history")
        @Operation(summary = "Get payment history", description = "Get paginated payment history for the authenticated user")
        public ResponseEntity<Page<PaymentHistoryResponse>> getPaymentHistory(
                        @AuthenticationPrincipal User user,
                        Pageable pageable) {

                Page<PaystackPayment> payments = paymentService.getPaymentsByUserId(user.getId(), pageable);
                Page<PaymentHistoryResponse> response = payments.map(PaymentHistoryResponse::fromEntity);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/transaction/{reference}")
        @Operation(summary = "Get payment by reference", description = "Get a specific payment by its reference")
        public ResponseEntity<PaymentHistoryResponse> getPaymentByReference(@PathVariable String reference) {
                return paymentService.getPaymentByReference(reference)
                                .map(PaymentHistoryResponse::fromEntity)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        // ==================== SUBSCRIPTION ENDPOINTS ====================

        @PostMapping("/subscription")
        @Operation(summary = "Create a subscription", description = "Create a new subscription for the authenticated user")
        public ResponseEntity<UnravelDocsResponse<PaystackSubscription>> createSubscription(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody CreateSubscriptionRequest request) {

                // Validate subscription eligibility
                userSubscriptionService.validateSubscriptionEligibility(user);

                PaystackSubscription subscription = subscriptionService.createSubscription(user, request);

                return ResponseEntity.status(HttpStatus.CREATED).body(new UnravelDocsResponse<>(
                                HttpStatus.CREATED.value(),
                                "success",
                                "Subscription created successfully",
                                subscription));
        }

        @GetMapping("/subscription/{subscriptionCode}")
        @Operation(summary = "Get subscription by code", description = "Get a subscription by its code")
        public ResponseEntity<PaystackSubscription> getSubscriptionByCode(@PathVariable String subscriptionCode) {
                return subscriptionService.getSubscriptionByCode(subscriptionCode)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/subscription/active")
        @Operation(summary = "Get active subscription", description = "Get the active subscription for the authenticated user")
        public ResponseEntity<PaystackSubscription> getActiveSubscription(@AuthenticationPrincipal User user) {
                return subscriptionService.getActiveSubscriptionByUserId(user.getId())
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/subscriptions")
        @Operation(summary = "Get subscription history", description = "Get paginated subscription history for the authenticated user")
        public ResponseEntity<Page<PaystackSubscription>> getSubscriptionHistory(
                        @AuthenticationPrincipal User user,
                        Pageable pageable) {

                Page<PaystackSubscription> subscriptions = subscriptionService.getSubscriptionsByUserId(user.getId(),
                                pageable);
                return ResponseEntity.ok(subscriptions);
        }

        @PostMapping("/subscription/{subscriptionCode}/enable")
        @Operation(summary = "Enable a subscription", description = "Enable a previously disabled subscription")
        public ResponseEntity<UnravelDocsResponse<PaystackSubscription>> enableSubscription(
                        @PathVariable String subscriptionCode,
                        @RequestParam String emailToken) {

                PaystackSubscription subscription = subscriptionService.enableSubscription(subscriptionCode,
                                emailToken);

                return ResponseEntity.ok(new UnravelDocsResponse<>(
                                HttpStatus.OK.value(),
                                "success",
                                "Subscription enabled successfully",
                                subscription));
        }

        @PostMapping("/subscription/{subscriptionCode}/disable")
        @Operation(summary = "Disable a subscription", description = "Disable (cancel) a subscription")
        public ResponseEntity<UnravelDocsResponse<PaystackSubscription>> disableSubscription(
                        @PathVariable String subscriptionCode,
                        @RequestParam String emailToken) {

                PaystackSubscription subscription = subscriptionService.disableSubscription(subscriptionCode,
                                emailToken);

                return ResponseEntity.ok(new UnravelDocsResponse<>(
                                HttpStatus.OK.value(),
                                "success",
                                "Subscription disabled successfully",
                                subscription));
        }

        // ==================== CALLBACK ENDPOINT ====================

        @GetMapping("/callback")
        @Operation(summary = "Payment callback", description = "Callback URL for Paystack payment redirect")
        public ResponseEntity<UnravelDocsResponse<TransactionData>> paymentCallback(
                        @RequestParam String reference,
                        @RequestParam(required = false) String trxref) {

                String ref = reference != null ? reference : trxref;
                TransactionData data = paymentService.verifyTransaction(ref);

                return ResponseEntity.ok(new UnravelDocsResponse<>(
                                HttpStatus.OK.value(),
                                "success",
                                "Payment " + data.getStatus(),
                                data));
        }
}
