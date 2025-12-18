package com.extractor.unraveldocs.payment.paystack.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;

/**
 * Global exception handler for Paystack-related operations
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.extractor.unraveldocs.payment.paystack")
public class PaystackGlobalExceptionHandler {

    @ExceptionHandler(PaystackPaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaystackPaymentException(PaystackPaymentException ex) {
        log.error("Paystack payment exception: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        "PaystackPaymentException",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(PaystackCustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaystackCustomerNotFoundException(PaystackCustomerNotFoundException ex) {
        log.error("Paystack customer not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage(),
                        "PaystackCustomerNotFoundException",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(PaystackSubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaystackSubscriptionNotFoundException(PaystackSubscriptionNotFoundException ex) {
        log.error("Paystack subscription not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage(),
                        "PaystackSubscriptionNotFoundException",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientErrorException(HttpClientErrorException ex) {
        log.error("Paystack API client error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = "Error communicating with Paystack: " + ex.getMessage();

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        status.value(),
                        message,
                        "PaystackApiClientException",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpServerErrorException(HttpServerErrorException ex) {
        log.error("Paystack API server error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Paystack service is temporarily unavailable. Please try again later.",
                        "PaystackApiServerException",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(RestClientException ex) {
        log.error("Paystack API connection error: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Unable to connect to Paystack service. Please try again later.",
                        "PaystackConnectionException",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(PaystackWebhookException.class)
    public ResponseEntity<ErrorResponse> handlePaystackWebhookException(PaystackWebhookException ex) {
        log.error("Paystack webhook error: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        "PaystackWebhookException",
                        OffsetDateTime.now()
                ));
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
        private String error;
        private OffsetDateTime timestamp;
    }
}
