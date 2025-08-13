package com.extractor.unraveldocs.payment.stripe.controller;

import com.extractor.unraveldocs.payment.stripe.dto.request.CheckoutRequestDto;
import com.extractor.unraveldocs.payment.stripe.dto.response.CheckoutResponseDto;
import com.extractor.unraveldocs.payment.stripe.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/stripe/subscription")
@RequiredArgsConstructor
@Tag(name = "Stripe Subscription", description = "Endpoints for managing Stripe subscriptions")
public class StripeSubscriptionController {
    private final StripeService stripeService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutResponseDto> createCheckoutSession(@RequestBody CheckoutRequestDto request) {
        try {
            Session session = stripeService.createCheckoutSession(request.getPriceId());
            CheckoutResponseDto response = new CheckoutResponseDto();
            response.setSessionId(session.getId());
            response.setSessionUrl(session.getUrl());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe exception occurred while creating checkout session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
