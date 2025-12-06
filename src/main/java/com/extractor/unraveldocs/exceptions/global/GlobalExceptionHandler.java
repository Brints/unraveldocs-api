package com.extractor.unraveldocs.exceptions.global;

import com.extractor.unraveldocs.exceptions.custom.*;
import com.extractor.unraveldocs.exceptions.response.ErrorResponse;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final ErrorResponse errorResponse = new ErrorResponse();

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<@NonNull Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName;
            String errorMessage = error.getDefaultMessage();
            if (error instanceof FieldError) {
                fieldName = ((FieldError) error).getField();
            } else {
                // Handle class-level constraints like @PasswordMatches
                fieldName = error.getObjectName().equals("signUpRequestDto") ? "confirmPassword" : error.getObjectName();
            }

            if ("role".equals(fieldName) && Objects.requireNonNull(error.getCode()).startsWith("typeMismatch")) {
                assert error instanceof FieldError;
                Object rejectedValue = ((FieldError) error).getRejectedValue();
                // Special handling for role validation errors
                errorMessage = "Invalid role: [" + rejectedValue + "]. Valid roles are: user, moderator, admin, " +
                        "super_admin";
            }
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<@NonNull ErrorResponse> handleNotFoundException(NotFoundException ex) {
        errorResponse.setStatusCode(HttpStatus.NOT_FOUND.value());
        errorResponse.setError(HttpStatus.NOT_FOUND.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<@NonNull ErrorResponse> handleConflictException(ConflictException ex) {
        errorResponse.setStatusCode(HttpStatus.CONFLICT.value());
        errorResponse.setError(HttpStatus.CONFLICT.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<@NonNull ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        errorResponse.setStatusCode(HttpStatus.FORBIDDEN.value());
        errorResponse.setError(HttpStatus.FORBIDDEN.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<@NonNull ErrorResponse> handleBadRequestException(BadRequestException ex) {
        errorResponse.setStatusCode(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<@NonNull ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        errorResponse.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        errorResponse.setError(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<@NonNull ErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException ex) {
        errorResponse.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        errorResponse.setError(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(TokenProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<@NonNull ErrorResponse> handleTokenProcessingException(TokenProcessingException ex) {
        errorResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public ResponseEntity<@NonNull ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        // Create a new ErrorResponse instance for thread safety if errorResponse is a shared field
        ErrorResponse specificErrorResponse = new ErrorResponse();
        specificErrorResponse.setStatusCode(HttpStatus.CONTENT_TOO_LARGE.value());
        specificErrorResponse.setError(HttpStatus.CONTENT_TOO_LARGE.getReasonPhrase());
        specificErrorResponse.setMessage(
                ex.getMessage() != null ? ex.getMessage() + " (Max size: 10MB)" :
                "Maximum upload size of " + ex.getMaxUploadSize() + " bytes exceeded."
                );
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(specificErrorResponse);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ResponseEntity<@NonNull ErrorResponse> handleTooManyRequestsException(TooManyRequestsException ex) {
        errorResponse.setStatusCode(HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.setError(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<@NonNull ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = "Invalid request payload.";
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            message = cause.getMessage();
        }
        errorResponse.setStatusCode(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<@NonNull ErrorResponse> handleGenericException(Exception ex) {
        errorResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
