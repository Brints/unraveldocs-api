package com.extractor.unraveldocs.payment.stripe.controller;

import com.extractor.unraveldocs.payment.stripe.dto.request.CheckoutRequestDto;
import com.extractor.unraveldocs.payment.stripe.dto.request.CreateSubscriptionRequest;
import com.extractor.unraveldocs.payment.stripe.dto.response.CheckoutResponseDto;
import com.extractor.unraveldocs.payment.stripe.dto.response.SubscriptionResponse;
import com.extractor.unraveldocs.payment.stripe.service.StripeService;
import com.extractor.unraveldocs.user.model.User;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.extractor.unraveldocs.subscription.service.UserSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing Stripe subscriptions
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stripe/subscription")
@RequiredArgsConstructor
@Tag(name = "Stripe Subscription", description = "Endpoints for managing Stripe subscriptions")
public class StripeSubscriptionController {

    private final StripeService stripeService;
    private final UserSubscriptionService userSubscriptionService;

    @PostMapping("/create-checkout-session")
    @Operation(summary = "Create a checkout session for subscription")
    public ResponseEntity<CheckoutResponseDto> createCheckoutSession(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CheckoutRequestDto request) {
        try {
            // Validate subscription eligibility
            userSubscriptionService.validateSubscriptionEligibility(user);

            Session session = stripeService.createCheckoutSession(
                    user,
                    request.getPriceId(),
                    request.getMode(),
                    request.getSuccessUrl(),
                    request.getCancelUrl(),
                    request.getQuantity(),
                    request.getTrialPeriodDays(),
                    request.getPromoCode(),
                    request.getMetadata());

            CheckoutResponseDto response = new CheckoutResponseDto(
                    session.getId(),
                    session.getUrl(),
                    session.getCustomer(),
                    session.getExpiresAt());

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe exception occurred while creating checkout session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/create")
    @Operation(summary = "Create a subscription directly")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        try {
            // Validate subscription eligibility
            userSubscriptionService.validateSubscriptionEligibility(user);

            SubscriptionResponse response = stripeService.createSubscription(user, request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to create subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Cancel a subscription")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @PathVariable String subscriptionId,
            @RequestParam(defaultValue = "false") boolean immediately) {
        try {
            SubscriptionResponse response = stripeService.cancelSubscription(subscriptionId, immediately);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to cancel subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{subscriptionId}/pause")
    @Operation(summary = "Pause a subscription")
    public ResponseEntity<SubscriptionResponse> pauseSubscription(@PathVariable String subscriptionId) {
        try {
            SubscriptionResponse response = stripeService.pauseSubscription(subscriptionId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to pause subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{subscriptionId}/resume")
    @Operation(summary = "Resume a paused subscription")
    public ResponseEntity<SubscriptionResponse> resumeSubscription(@PathVariable String subscriptionId) {
        try {
            SubscriptionResponse response = stripeService.resumeSubscription(subscriptionId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to resume subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{subscriptionId}")
    @Operation(summary = "Get subscription details")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable String subscriptionId) {
        try {
            SubscriptionResponse response = stripeService.getSubscription(subscriptionId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to retrieve subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
