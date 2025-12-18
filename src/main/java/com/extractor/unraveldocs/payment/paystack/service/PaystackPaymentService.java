package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.paystack.config.PaystackConfig;
import com.extractor.unraveldocs.payment.paystack.dto.request.InitializeTransactionRequest;
import com.extractor.unraveldocs.payment.paystack.dto.response.*;
import com.extractor.unraveldocs.payment.paystack.exception.PaystackPaymentException;
import com.extractor.unraveldocs.payment.paystack.model.PaystackCustomer;
import com.extractor.unraveldocs.payment.paystack.model.PaystackPayment;
import com.extractor.unraveldocs.payment.paystack.repository.PaystackPaymentRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for Paystack payment operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackPaymentService {

    private final RestClient paystackRestClient;
    private final PaystackConfig paystackConfig;
    private final PaystackCustomerService customerService;
    private final PaystackPaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitize;

    /**
     * Initialize a transaction for one-time payment or subscription
     */
    @Transactional
    public InitializeTransactionData initializeTransaction(
            User user, InitializeTransactionRequest request
    ) {
        try {
            // Get or create customer
            PaystackCustomer customer = customerService.getOrCreateCustomer(user);

            // Generate unique reference if not provided
            String reference = request.getReference() != null ?
                    request.getReference() : generateReference();

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", user.getEmail());
            requestBody.put("amount", request.getAmount());
            requestBody.put("reference", reference);
            requestBody.put("currency", request.getCurrency() != null ? request.getCurrency() : paystackConfig.getDefaultCurrency());
            requestBody.put(
                    "callback_url", request.getCallbackUrl() != null ?
                            request.getCallbackUrl() :
                            paystackConfig.getCallbackUrl()
            );

            if (request.getPlanCode() != null) {
                requestBody.put("plan", request.getPlanCode());
            }

            if (request.getChannels() != null && request.getChannels().length > 0) {
                requestBody.put("channels", request.getChannels());
            }

            Map<String, Object> metadata;
            if (request.getMetadata() != null) {
                metadata = new HashMap<>(request.getMetadata());
            } else {
                metadata = new HashMap<>();
            }
            metadata.put("user_id", user.getId());
            metadata.put("customer_code", customer.getCustomerCode());
            requestBody.put("metadata", metadata);

            String responseBody = paystackRestClient.post()
                    .uri("/transaction/initialize")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<InitializeTransactionData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {
                    }
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException(
                        "Failed to initialize transaction: " + response.getMessage());
            }

            InitializeTransactionData data = response.getData();

            // Save payment record
            PaymentType paymentType = request.getPlanCode() != null ? PaymentType.SUBSCRIPTION : PaymentType.ONE_TIME;
            BigDecimal amount = BigDecimal.valueOf(request.getAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            PaystackPayment payment = PaystackPayment.builder()
                    .user(user)
                    .paystackCustomer(customer)
                    .reference(data.getReference())
                    .accessCode(data.getAccessCode())
                    .authorizationUrl(data.getAuthorizationUrl())
                    .paymentType(paymentType)
                    .status(PaymentStatus.PENDING)
                    .amount(amount)
                    .currency(request.getCurrency() != null ? request.getCurrency() : paystackConfig.getDefaultCurrency())
                    .planCode(request.getPlanCode())
                    .build();

            if (request.getMetadata() != null) {
                try {
                    payment.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize payment metadata: {}", e.getMessage());
                }
            }

            paymentRepository.save(payment);
            log.info("Initialized transaction {} for user {}", sanitize.sanitizeLogging(reference), sanitize.sanitizeLogging(user.getId()));

            return data;
        } catch (Exception e) {
            log.error("Failed to initialize transaction for user {}: {}", sanitize.sanitizeLogging(user.getId()), e.getMessage());
            throw new PaystackPaymentException("Failed to initialize transaction", e);
        }
    }

    /**
     * Verify a transaction
     */
    @Transactional
    public TransactionData verifyTransaction(String reference) {
        try {
            String responseBody = paystackRestClient.get()
                    .uri("/transaction/verify/{reference}", reference)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<TransactionData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {
                    }
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to verify transaction: " + response.getMessage());
            }

            TransactionData data = response.getData();

            // Update payment record
            paymentRepository.findByReference(reference).ifPresent(payment -> {
                payment.setTransactionId(data.getId());
                payment.setStatus(mapPaystackStatusToPaymentStatus(data.getStatus()));
                payment.setChannel(data.getChannel());
                payment.setGatewayResponse(data.getGatewayResponse());
                payment.setIpAddress(data.getIpAddress());

                if (data.getFees() != null) {
                    payment.setFees(BigDecimal.valueOf(data.getFees()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }

                if (data.getAuthorization() != null) {
                    payment.setAuthorizationCode(data.getAuthorization().getAuthorizationCode());
                }

                if (data.getPaidAt() != null) {
                    payment.setPaidAt(parsePaystackDateTime(data.getPaidAt()));
                }

                paymentRepository.save(payment);
                log.info("Updated payment {} with status {}", sanitize.sanitizeLogging(reference), sanitize.sanitizeLogging(data.getStatus()));
            });

            return data;
        } catch (Exception e) {
            log.error("Failed to verify transaction {}: {}", sanitize.sanitizeLogging(reference), e.getMessage());
            throw new PaystackPaymentException("Failed to verify transaction", e);
        }
    }

    /**
     * Charge authorization (recurring payment)
     */
    @Transactional
    public TransactionData chargeAuthorization(User user, String authorizationCode, Long amount, String currency) {
        try {
            PaystackCustomer customer = customerService.getCustomerByUserId(user.getId());
            String reference = generateReference();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", user.getEmail());
            requestBody.put("amount", amount);
            requestBody.put("authorization_code", authorizationCode);
            requestBody.put("reference", reference);
            requestBody.put("currency", currency != null ? currency : paystackConfig.getDefaultCurrency());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("user_id", user.getId());
            metadata.put("customer_code", customer.getCustomerCode());
            requestBody.put("metadata", metadata);

            String responseBody = paystackRestClient.post()
                    .uri("/transaction/charge_authorization")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<TransactionData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {
                    }
            );

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to charge authorization: " + response.getMessage());
            }

            TransactionData data = response.getData();

            // Save payment record
            BigDecimal paymentAmount = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            PaystackPayment payment = PaystackPayment.builder()
                    .user(user)
                    .paystackCustomer(customer)
                    .transactionId(data.getId())
                    .reference(reference)
                    .authorizationCode(authorizationCode)
                    .paymentType(PaymentType.SUBSCRIPTION)
                    .status(mapPaystackStatusToPaymentStatus(data.getStatus()))
                    .amount(paymentAmount)
                    .currency(currency != null ? currency : paystackConfig.getDefaultCurrency())
                    .channel(data.getChannel())
                    .gatewayResponse(data.getGatewayResponse())
                    .build();

            paymentRepository.save(payment);
            log.info("Charged authorization {} for user {}, reference: {}", sanitize.sanitizeLogging(authorizationCode), sanitize.sanitizeLogging(user.getId()), sanitize.sanitizeLogging(reference));

            return data;
        } catch (Exception e) {
            log.error("Failed to charge authorization for user {}: {}", sanitize.sanitizeLogging(user.getId()), e.getMessage());
            throw new PaystackPaymentException("Failed to charge authorization", e);
        }
    }

    /**
     * Get payment by reference
     */
    public Optional<PaystackPayment> getPaymentByReference(String reference) {
        return paymentRepository.findByReference(reference);
    }

    /**
     * Get payments for a user
     */
    public Page<PaystackPayment> getPaymentsByUserId(String userId, Pageable pageable) {
        return paymentRepository.findByUser_Id(userId, pageable);
    }

    /**
     * Get payments by status
     */
    public Page<PaystackPayment> getPaymentsByUserIdAndStatus(String userId, PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByUser_IdAndStatus(userId, status, pageable);
    }

    /**
     * Check if payment exists
     */
    public boolean paymentExists(String reference) {
        return paymentRepository.existsByReference(reference);
    }

    /**
     * Update payment status
     */
    @Transactional
    public void updatePaymentStatus(String reference, PaymentStatus status, String failureMessage) {
        paymentRepository.findByReference(reference).ifPresent(payment -> {
            payment.setStatus(status);
            if (failureMessage != null) {
                payment.setFailureMessage(failureMessage);
            }
            paymentRepository.save(payment);
            log.info("Updated payment {} status to {}", sanitize.sanitizeLogging(reference), sanitize.sanitizeLogging(String.valueOf(status)));
        });
    }

    /**
     * Record refund
     */
    @Transactional
    public void recordRefund(String reference, BigDecimal refundAmount) {
        paymentRepository.findByReference(reference).ifPresent(payment -> {
            BigDecimal currentRefunded = payment.getAmountRefunded() != null ? payment.getAmountRefunded() : BigDecimal.ZERO;
            payment.setAmountRefunded(currentRefunded.add(refundAmount));

            if (payment.getAmountRefunded().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }

            paymentRepository.save(payment);
            log.info("Recorded refund of {} for payment {}", sanitize.sanitizeLogging(String.valueOf(refundAmount)), sanitize.sanitizeLogging(reference));
        });
    }

    /**
     * Generate unique reference
     */
    private String generateReference() {
        return "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Map Paystack status to PaymentStatus enum
     */
    private PaymentStatus mapPaystackStatusToPaymentStatus(String paystackStatus) {
        if (paystackStatus == null) {
            return PaymentStatus.PENDING;
        }

        return switch (paystackStatus.toLowerCase()) {
            case "success" -> PaymentStatus.SUCCEEDED;
            case "failed" -> PaymentStatus.FAILED;
            case "abandoned" -> PaymentStatus.CANCELED;
            case "pending" -> PaymentStatus.PENDING;
            case "processing" -> PaymentStatus.PROCESSING;
            case "reversed" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }

    /**
     * Parse Paystack datetime string
     */
    private OffsetDateTime parsePaystackDateTime(String dateTimeString) {
        try {
            return OffsetDateTime.parse(dateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeString);
            return null;
        }
    }
}
