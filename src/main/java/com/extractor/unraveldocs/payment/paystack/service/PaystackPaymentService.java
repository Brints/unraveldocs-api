package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.coupon.dto.request.ApplyCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.DiscountCalculationData;
import com.extractor.unraveldocs.coupon.exception.InvalidCouponException;
import com.extractor.unraveldocs.coupon.service.CouponValidationService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.paystack.config.PaystackConfig;
import com.extractor.unraveldocs.payment.paystack.dto.request.ChargeAuthorizationPayload;
import com.extractor.unraveldocs.payment.paystack.dto.request.InitializeTransactionRequest;
import com.extractor.unraveldocs.payment.paystack.dto.request.PaystackTransactionPayload;
import com.extractor.unraveldocs.payment.paystack.dto.request.TransactionMetadata;
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
import java.util.Optional;
import java.util.UUID;

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
    private final CouponValidationService couponValidationService;

    /**
     * Initialize a transaction for one-time payment or subscription
     */
    @Transactional
    public InitializeTransactionData initializeTransaction(
            User user, InitializeTransactionRequest request) {
        try {
            // Get or create customer
            PaystackCustomer customer = customerService.getOrCreateCustomer(user);

            // Generate unique reference if not provided
            String reference = request.getReference() != null ? request.getReference() : generateReference();

            // Track coupon discount info
            BigDecimal originalAmount = request.getAmount();
            BigDecimal finalAmount = request.getAmount();
            BigDecimal discountAmount = BigDecimal.ZERO;
            String appliedCouponCode = null;

            // Validate and apply coupon if provided
            if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
                log.info("Validating coupon code: {} for user: {}",
                        sanitize.sanitizeLogging(request.getCouponCode()),
                        sanitize.sanitizeLogging(user.getId()));

                ApplyCouponRequest couponRequest = ApplyCouponRequest.builder()
                        .couponCode(request.getCouponCode())
                        .amount(originalAmount)
                        .subscriptionPlan(request.getPlanCode())
                        .build();

                DiscountCalculationData discountData = couponValidationService.applyCouponToAmount(couponRequest, user);

                if (discountData == null) {
                    throw new InvalidCouponException(
                            "Coupon validation failed for code: " + request.getCouponCode());
                }

                if (!discountData.isMinPurchaseRequirementMet()) {
                    throw new InvalidCouponException(
                            "Minimum purchase amount of " + discountData.getMinPurchaseAmount() + " not met");
                }

                finalAmount = discountData.getFinalAmount();
                discountAmount = discountData.getDiscountAmount();
                appliedCouponCode = discountData.getCouponCode();

                log.info("Coupon applied. Original: {}, Discount: {}, Final: {}",
                        sanitize.sanitizeLoggingObject(originalAmount),
                        sanitize.sanitizeLoggingObject(discountAmount),
                        sanitize.sanitizeLoggingObject(finalAmount));
            }

            long amountInKobo = finalAmount
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            String internalPlanCode = request.getPlanCode();

            // Build metadata DTO
            TransactionMetadata metadata = TransactionMetadata.builder()
                    .userId(user.getId())
                    .customerCode(customer.getCustomerCode())
                    .planCode(internalPlanCode != null && !internalPlanCode.isBlank() ? internalPlanCode : null)
                    .couponCode(appliedCouponCode)
                    .originalAmount(appliedCouponCode != null ? originalAmount.toString() : null)
                    .discountAmount(appliedCouponCode != null ? discountAmount.toString() : null)
                    .build();

            // Build request payload DTO
            PaystackTransactionPayload payload = PaystackTransactionPayload.builder()
                    .email(user.getEmail())
                    .amount(amountInKobo)
                    .reference(reference)
                    .currency(request.getCurrency() != null ? request.getCurrency() : paystackConfig.getDefaultCurrency())
                    .callbackUrl(request.getCallbackUrl() != null ? request.getCallbackUrl() : paystackConfig.getCallbackUrl())
                    .plan(internalPlanCode != null && internalPlanCode.startsWith("PLN_") ? internalPlanCode : null)
                    .channels(request.getChannels() != null && request.getChannels().length > 0 ? request.getChannels() : null)
                    .metadata(metadata)
                    .build();

            String responseBody = paystackRestClient.post()
                    .uri("/transaction/initialize")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<InitializeTransactionData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {
                    });

            if (!response.isStatus()) {
                throw new PaystackPaymentException(
                        "Failed to initialize transaction: " + response.getMessage());
            }

            InitializeTransactionData data = response.getData();

            PaymentType paymentType = request.getPlanCode() != null ? PaymentType.SUBSCRIPTION : PaymentType.ONE_TIME;
            BigDecimal amountInMajorUnits = finalAmount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal originalAmountInMajorUnits = appliedCouponCode != null
                    ? originalAmount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : null;
            BigDecimal discountAmountInMajorUnits = appliedCouponCode != null
                    ? discountAmount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : null;

            PaystackPayment payment = PaystackPayment.builder()
                    .user(user)
                    .paystackCustomer(customer)
                    .reference(data.getReference())
                    .accessCode(data.getAccessCode())
                    .authorizationUrl(data.getAuthorizationUrl())
                    .paymentType(paymentType)
                    .status(PaymentStatus.PENDING)
                    .amount(amountInMajorUnits)
                    .currency(
                            request.getCurrency() != null ? request.getCurrency() : paystackConfig.getDefaultCurrency())
                    .planCode(internalPlanCode)
                    .couponCode(appliedCouponCode)
                    .originalAmount(originalAmountInMajorUnits)
                    .discountAmount(discountAmountInMajorUnits)
                    .build();

            // Always store metadata - contains plan_code, user_id, customer_code, etc.
            try {
                payment.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize payment metadata: {}", e.getMessage());
            }

            paymentRepository.save(payment);

            if (appliedCouponCode != null) {
                log.info("Initialized transaction {} for user {} with coupon {}. Original: {}, Final: {}",
                        sanitize.sanitizeLogging(reference),
                        sanitize.sanitizeLogging(user.getId()),
                        sanitize.sanitizeLogging(appliedCouponCode),
                        sanitize.sanitizeLoggingObject(originalAmount),
                        sanitize.sanitizeLoggingObject(finalAmount));
            } else {
                log.info("Initialized transaction {} for user {}",
                        sanitize.sanitizeLogging(reference),
                        sanitize.sanitizeLogging(user.getId()));
            }

            return data;
        } catch (InvalidCouponException e) {
            log.warn("Coupon validation failed for user {}: {}", sanitize.sanitizeLogging(user.getId()),
                    e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to initialize transaction for user {}: {}", sanitize.sanitizeLogging(user.getId()),
                    e.getMessage());
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
                    });

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
                    payment.setFees(BigDecimal.valueOf(data.getFees()).divide(BigDecimal.valueOf(100), 2,
                            RoundingMode.HALF_UP));
                }

                if (data.getAuthorization() != null) {
                    payment.setAuthorizationCode(data.getAuthorization().getAuthorizationCode());
                }

                if (data.getPaidAt() != null) {
                    payment.setPaidAt(parsePaystackDateTime(data.getPaidAt()));
                }

                paymentRepository.save(payment);
                log.info("Updated payment {} with status {}", sanitize.sanitizeLogging(reference),
                        sanitize.sanitizeLogging(data.getStatus()));
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

            // Build metadata DTO
            TransactionMetadata metadata = TransactionMetadata.builder()
                    .userId(user.getId())
                    .customerCode(customer.getCustomerCode())
                    .build();

            // Build request payload DTO
            ChargeAuthorizationPayload payload = ChargeAuthorizationPayload.builder()
                    .email(user.getEmail())
                    .amount(amount)
                    .authorizationCode(authorizationCode)
                    .reference(reference)
                    .currency(currency != null ? currency : paystackConfig.getDefaultCurrency())
                    .metadata(metadata)
                    .build();

            String responseBody = paystackRestClient.post()
                    .uri("/transaction/charge_authorization")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            PaystackResponse<TransactionData> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {
                    });

            if (!response.isStatus()) {
                throw new PaystackPaymentException("Failed to charge authorization: " + response.getMessage());
            }

            TransactionData data = response.getData();

            // Save payment record
            BigDecimal paymentAmount = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100), 2,
                    RoundingMode.HALF_UP);

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
            log.info("Charged authorization {} for user {}, reference: {}", sanitize.sanitizeLogging(authorizationCode),
                    sanitize.sanitizeLogging(user.getId()), sanitize.sanitizeLogging(reference));

            return data;
        } catch (Exception e) {
            log.error("Failed to charge authorization for user {}: {}", sanitize.sanitizeLogging(user.getId()),
                    e.getMessage());
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
            log.info("Updated payment {} status to {}", sanitize.sanitizeLogging(reference),
                    sanitize.sanitizeLogging(String.valueOf(status)));
        });
    }

    /**
     * Record refund
     */
    @Transactional
    public void recordRefund(String reference, BigDecimal refundAmount) {
        paymentRepository.findByReference(reference).ifPresent(payment -> {
            BigDecimal currentRefunded = payment.getAmountRefunded() != null ? payment.getAmountRefunded()
                    : BigDecimal.ZERO;
            payment.setAmountRefunded(currentRefunded.add(refundAmount));

            if (payment.getAmountRefunded().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }

            paymentRepository.save(payment);
            log.info("Recorded refund of {} for payment {}", sanitize.sanitizeLogging(String.valueOf(refundAmount)),
                    sanitize.sanitizeLogging(reference));
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
