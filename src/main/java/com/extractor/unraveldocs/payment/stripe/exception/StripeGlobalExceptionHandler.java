package com.extractor.unraveldocs.payment.stripe.exception;

import com.stripe.exception.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

/**
 * Global exception handler for Stripe-related operations
 */
@Slf4j
@RestControllerAdvice
public class StripeGlobalExceptionHandler {

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ErrorResponse> handleStripeException(StripeException ex) {
        log.error("Stripe API error occurred: {}", ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An error occurred while processing your payment";

        switch (ex) {
            case CardException cardException -> {
                status = HttpStatus.BAD_REQUEST;
                message = "Your card was declined: " + ex.getMessage();
            }
            case RateLimitException rateLimitException -> {
                status = HttpStatus.TOO_MANY_REQUESTS;
                message = "Too many requests. Please try again later.";
            }
            case InvalidRequestException invalidRequestException -> {
                status = HttpStatus.BAD_REQUEST;
                message = "Invalid payment request: " + ex.getMessage();
            }
            case AuthenticationException authenticationException -> {
                status = HttpStatus.UNAUTHORIZED;
                message = "Payment authentication failed";
            }
            case ApiConnectionException apiConnectionException -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                message = "Unable to connect to payment service";
            }
            default -> {
            }
        }
        
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        status.value(),
                        message,
                        ex.getClass().getSimpleName(),
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(StripePaymentException.class)
    public ResponseEntity<ErrorResponse> handleStripePaymentException(StripePaymentException ex) {
        log.error("Stripe payment exception: {}", ex.getMessage(), ex);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        "StripePaymentException",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFoundException(CustomerNotFoundException ex) {
        log.error("Customer not found: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage(),
                        "CustomerNotFoundException",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        log.error("Payment not found: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage(),
                        "PaymentNotFoundException",
                        OffsetDateTime.now()
                ));
    }

    /**
     * Error response DTO
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
        private String error;
        private OffsetDateTime timestamp;
    }
}
