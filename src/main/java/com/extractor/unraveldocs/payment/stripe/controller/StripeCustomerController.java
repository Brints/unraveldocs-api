package com.extractor.unraveldocs.payment.stripe.controller;

import com.extractor.unraveldocs.payment.stripe.dto.response.CustomerResponse;
import com.extractor.unraveldocs.payment.stripe.service.StripeCustomerService;
import com.extractor.unraveldocs.user.model.User;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for customer management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stripe/customer")
@RequiredArgsConstructor
@Tag(name = "Stripe Customer", description = "Endpoints for customer management")
public class StripeCustomerController {

    private final StripeCustomerService customerService;

    @GetMapping("/details")
    @Operation(summary = "Get customer details with payment methods")
    public ResponseEntity<CustomerResponse> getCustomerDetails(@AuthenticationPrincipal User user) {
        try {
            CustomerResponse response = customerService.getCustomerDetails(user.getId());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to get customer details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/payment-method/attach")
    @Operation(summary = "Attach a payment method to customer")
    public ResponseEntity<Void> attachPaymentMethod(
            @AuthenticationPrincipal User user,
            @RequestParam String paymentMethodId) {
        try {
            customerService.attachPaymentMethod(user.getId(), paymentMethodId);
            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            log.error("Failed to attach payment method: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/payment-method/set-default")
    @Operation(summary = "Set default payment method")
    public ResponseEntity<Void> setDefaultPaymentMethod(
            @AuthenticationPrincipal User user,
            @RequestParam String paymentMethodId) {
        try {
            customerService.setDefaultPaymentMethod(user.getId(), paymentMethodId);
            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            log.error("Failed to set default payment method: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
