package com.extractor.unraveldocs.payment.paypal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Global exception handler for PayPal payment operations.
 */
@Slf4j
@Order(1)
@RestControllerAdvice(basePackages = "com.extractor.unraveldocs.payment.paypal")
public class PayPalGlobalExceptionHandler {

    @ExceptionHandler(PayPalPaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePayPalPaymentException(PayPalPaymentException ex) {
        log.error("PayPal payment error: {}", ex.getMessage(), ex);

        Map<String, Object> response = Map.of(
                "status", false,
                "error", "PayPal Payment Error",
                "message", ex.getMessage(),
                "errorCode", ex.getErrorCode() != null ? ex.getErrorCode() : "PAYMENT_ERROR",
                "timestamp", OffsetDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(PayPalSubscriptionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSubscriptionNotFound(PayPalSubscriptionNotFoundException ex) {
        log.warn("PayPal subscription not found: {}", ex.getMessage());

        Map<String, Object> response = Map.of(
                "status", false,
                "error", "Subscription Not Found",
                "message", ex.getMessage(),
                "timestamp", OffsetDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(PayPalWebhookException.class)
    public ResponseEntity<Map<String, Object>> handleWebhookException(PayPalWebhookException ex) {
        log.error("PayPal webhook error: {}", ex.getMessage(), ex);

        Map<String, Object> response = Map.of(
                "status", false,
                "error", "Webhook Processing Error",
                "message", ex.getMessage(),
                "timestamp", OffsetDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
