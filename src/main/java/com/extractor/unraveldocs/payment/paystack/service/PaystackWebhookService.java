package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.service.CouponValidationService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.paystack.config.PaystackConfig;
import com.extractor.unraveldocs.payment.paystack.dto.response.SubscriptionData;
import com.extractor.unraveldocs.payment.paystack.dto.response.TransactionData;
import com.extractor.unraveldocs.payment.paystack.dto.webhook.PaystackWebhookEvent;
import com.extractor.unraveldocs.payment.paystack.exception.PaystackWebhookException;
import com.extractor.unraveldocs.payment.paystack.model.PaystackPayment;
import com.extractor.unraveldocs.payment.paystack.repository.PaystackPaymentRepository;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.events.ReceiptEventPublisher;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.SubscriptionPlan;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.SubscriptionPlanRepository;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Service for handling Paystack webhook events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackWebhookService {

    private static final String HMAC_SHA512 = "HmacSHA512";

    private final PaystackConfig paystackConfig;
    private final PaystackPaymentService paymentService;
    private final PaystackSubscriptionService subscriptionService;
    private final PaystackPaymentRepository paymentRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitize;
    private final CouponValidationService couponValidationService;
    private final CouponRepository couponRepository;

    private ReceiptEventPublisher receiptEventPublisher;

    @Autowired(required = false)
    public void setReceiptEventPublisher(ReceiptEventPublisher receiptEventPublisher) {
        this.receiptEventPublisher = receiptEventPublisher;
    }

    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (paystackConfig.getWebhookSecret() == null || paystackConfig.getWebhookSecret().isEmpty()) {
            log.warn("Webhook secret not configured, skipping signature verification");
            return true;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    paystackConfig.getSecretKey().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA512);
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = bytesToHex(hash);

            return computedSignature.equalsIgnoreCase(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to verify webhook signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Process webhook event
     */
    @Transactional
    public void processWebhookEvent(PaystackWebhookEvent event) {
        String eventType = event.getEvent();
        log.info("Processing Paystack webhook event: {}", sanitize.sanitizeLogging(eventType));

        try {
            switch (eventType) {
                case "charge.success" -> handleChargeSuccess(event.getData());
                case "charge.failed" -> handleChargeFailed(event.getData());
                case "subscription.create" -> handleSubscriptionCreate(event.getData());
                case "subscription.disable" -> handleSubscriptionDisable(event.getData());
                case "subscription.not_renew" -> handleSubscriptionNotRenew(event.getData());
                case "subscription.expiring_cards" -> handleSubscriptionExpiringCards(event.getData());
                case "invoice.create" -> handleInvoiceCreate(event.getData());
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(event.getData());
                case "invoice.update" -> handleInvoiceUpdate(event.getData());
                case "transfer.success" -> handleTransferSuccess(event.getData());
                case "transfer.failed" -> handleTransferFailed(event.getData());
                case "refund.processed" -> handleRefundProcessed(event.getData());
                default -> log.info("Unhandled webhook event type: {}", sanitize.sanitizeLogging(eventType));
            }
        } catch (Exception e) {
            log.error("Error processing webhook event {}: {}", sanitize.sanitizeLogging(eventType), e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process webhook event: " + eventType, e);
        }
    }

    /**
     * Handle successful charge
     */
    private void handleChargeSuccess(Map<String, Object> data) {
        try {
            TransactionData transactionData = objectMapper.convertValue(data, TransactionData.class);
            String reference = transactionData.getReference();

            log.info("Processing charge.success for reference: {}", sanitize.sanitizeLogging(reference));

            paymentRepository.findByReference(reference).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setTransactionId(transactionData.getId());
                payment.setChannel(transactionData.getChannel());
                payment.setGatewayResponse(transactionData.getGatewayResponse());

                if (transactionData.getFees() != null) {
                    payment.setFees(BigDecimal.valueOf(transactionData.getFees())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }

                if (transactionData.getAuthorization() != null) {
                    payment.setAuthorizationCode(transactionData.getAuthorization().getAuthorizationCode());
                }

                paymentRepository.save(payment);
                log.info("Updated payment {} to SUCCEEDED", sanitize.sanitizeLogging(reference));

                // Update user subscription for subscription payments
                if (payment.getPaymentType() == PaymentType.SUBSCRIPTION) {
                    updateUserSubscriptionFromPayment(payment);
                }

                // Record coupon usage if a coupon was applied
                recordCouponUsageIfApplicable(payment);

                // Generate receipt
                generateReceipt(payment, transactionData);
            });
        } catch (Exception e) {
            log.error("Failed to handle charge.success: {}", e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process charge.success", e);
        }
    }

    /**
     * Update user subscription from payment data after successful charge.
     * This ensures the user_subscription table is updated when a payment succeeds.
     */
    private void updateUserSubscriptionFromPayment(PaystackPayment payment) {
        try {
            User user = payment.getUser();

            // Try to get plan code from metadata first (internal plan code like "STARTER_MONTHLY")
            // then fall back to payment.getPlanCode() which may be a Paystack plan code
            String planCode = extractPlanFromMetadata(payment.getMetadata());
            if (planCode == null || planCode.isBlank()) {
                planCode = payment.getPlanCode();
            }

            if (planCode == null || planCode.isBlank()) {
                log.warn("No plan code found for subscription payment: {}. " +
                        "Subscription update skipped. Ensure planCode is sent in initialize request or metadata.",
                        sanitize.sanitizeLogging(payment.getReference()));
                return;
            }

            log.info("Attempting to update user subscription for payment {} with plan code: {}",
                    sanitize.sanitizeLogging(payment.getReference()),
                    sanitize.sanitizeLogging(planCode));

            // Try to find the subscription plan by name
            SubscriptionPlan plan = findSubscriptionPlan(planCode);
            if (plan == null) {
                log.error("Subscription plan not found for code: {}. Available lookup methods tried: " +
                        "enum name match, Paystack plan code match, partial name match.",
                        sanitize.sanitizeLogging(planCode));
                return;
            }

            // Get or create user subscription
            UserSubscription userSubscription = userSubscriptionRepository.findByUserId(user.getId())
                    .orElseGet(() -> {
                        log.info("Creating new user subscription for user: {}",
                                sanitize.sanitizeLogging(user.getId()));
                        UserSubscription newSubscription = new UserSubscription();
                        newSubscription.setUser(user);
                        return newSubscription;
                    });

            // Update subscription details
            userSubscription.setPlan(plan);
            userSubscription.setStatus("active");
            userSubscription.setCurrentPeriodStart(OffsetDateTime.now());

            // Calculate period end based on billing interval
            OffsetDateTime periodEnd = calculatePeriodEnd(plan);
            userSubscription.setCurrentPeriodEnd(periodEnd);

            // Set auto-renew based on payment type
            userSubscription.setAutoRenew(true);

            // Link to Paystack subscription if available
            if (payment.getSubscriptionCode() != null) {
                userSubscription.setPaymentGatewaySubscriptionId(payment.getSubscriptionCode());
            }

            userSubscriptionRepository.save(userSubscription);
            log.info("Successfully updated user subscription for user {} with plan {} (period: {} to {})",
                    sanitize.sanitizeLogging(user.getId()),
                    sanitize.sanitizeLogging(plan.getName().getPlanName()),
                    userSubscription.getCurrentPeriodStart(),
                    userSubscription.getCurrentPeriodEnd());

        } catch (Exception e) {
            log.error("Failed to update user subscription for payment {}: {}",
                    sanitize.sanitizeLogging(payment.getReference()), e.getMessage(), e);
            // Don't rethrow - user subscription update failure shouldn't fail the webhook
        }
    }

    /**
     * Extract plan code from payment metadata JSON.
     */
    private String extractPlanFromMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
            // Try common metadata keys for plan information
            if (metadata.containsKey("plan_code")) {
                return String.valueOf(metadata.get("plan_code"));
            }
            if (metadata.containsKey("planCode")) {
                return String.valueOf(metadata.get("planCode"));
            }
            if (metadata.containsKey("plan")) {
                return String.valueOf(metadata.get("plan"));
            }
            if (metadata.containsKey("subscription_plan")) {
                return String.valueOf(metadata.get("subscription_plan"));
            }
        } catch (Exception e) {
            log.debug("Could not parse metadata for plan extraction: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Find subscription plan by plan code.
     * The plan code can be a SubscriptionPlans enum name or a Paystack plan code.
     * Also supports partial name matching for flexibility.
     */
    private SubscriptionPlan findSubscriptionPlan(String planCode) {
        log.debug("Searching for subscription plan with code: {}", sanitize.sanitizeLogging(planCode));

        // First, try to find by enum name (e.g., "STARTER_MONTHLY", "Starter_Monthly")
        try {
            SubscriptionPlans planEnum = SubscriptionPlans.fromString(planCode);
            SubscriptionPlan plan = subscriptionPlanRepository.findByName(planEnum).orElse(null);
            if (plan != null) {
                log.debug("Found plan by enum name: {}", plan.getName());
                return plan;
            }
        } catch (IllegalArgumentException e) {
            // Not a valid enum name, try other methods
            log.debug("Plan code {} is not a valid enum name, trying other methods",
                    sanitize.sanitizeLogging(planCode));
        }

        // Try to find by Paystack plan code (exact match)
        SubscriptionPlan byPaystackCode = subscriptionPlanRepository.findAll().stream()
                .filter(plan -> planCode.equals(plan.getPaystackPlanCode()))
                .findFirst()
                .orElse(null);
        if (byPaystackCode != null) {
            log.debug("Found plan by Paystack plan code: {}", byPaystackCode.getName());
            return byPaystackCode;
        }

        // Try partial/case-insensitive match on plan name
        String normalizedCode = planCode.toUpperCase().replace(" ", "_").replace("-", "_");
        SubscriptionPlan byPartialMatch = subscriptionPlanRepository.findAll().stream()
                .filter(plan -> {
                    String planName = plan.getName().name().toUpperCase();
                    return planName.contains(normalizedCode) || normalizedCode.contains(planName);
                })
                .findFirst()
                .orElse(null);
        if (byPartialMatch != null) {
            log.debug("Found plan by partial name match: {}", byPartialMatch.getName());
            return byPartialMatch;
        }

        log.warn("No subscription plan found for code: {}", sanitize.sanitizeLogging(planCode));
        return null;
    }

    /**
     * Calculate the subscription period end based on the plan's billing interval.
     */
    private OffsetDateTime calculatePeriodEnd(SubscriptionPlan plan) {
        OffsetDateTime now = OffsetDateTime.now();
        BillingIntervalUnit intervalUnit = plan.getBillingIntervalUnit();
        int intervalValue = plan.getBillingIntervalValue();

        return switch (intervalUnit) {
            case WEEK -> now.plusWeeks(intervalValue);
            case MONTH -> now.plusMonths(intervalValue);
            case YEAR -> now.plusYears(intervalValue);
        };
    }

    /**
     * Handle failed charge
     */
    private void handleChargeFailed(Map<String, Object> data) {
        try {
            TransactionData transactionData = objectMapper.convertValue(data, TransactionData.class);
            String reference = transactionData.getReference();

            log.info("Processing charge.failed for reference: {}", sanitize.sanitizeLogging(reference));

            paymentRepository.findByReference(reference).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setGatewayResponse(transactionData.getGatewayResponse());
                payment.setFailureMessage(transactionData.getGatewayResponse());
                paymentRepository.save(payment);
                log.info("Updated payment {} to FAILED", sanitize.sanitizeLogging(reference));
            });
        } catch (Exception e) {
            log.error("Failed to handle charge.failed: {}", e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process charge.failed", e);
        }
    }

    /**
     * Handle subscription creation
     */
    private void handleSubscriptionCreate(Map<String, Object> data) {
        try {
            SubscriptionData subscriptionData = objectMapper.convertValue(data, SubscriptionData.class);
            log.info("Processing subscription.create for: {}",
                    sanitize.sanitizeLogging(subscriptionData.getSubscriptionCode()));

            subscriptionService.updateSubscriptionFromWebhook(subscriptionData);
        } catch (Exception e) {
            log.error("Failed to handle subscription.create: {}", e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process subscription.create", e);
        }
    }

    /**
     * Handle subscription disable
     */
    private void handleSubscriptionDisable(Map<String, Object> data) {
        try {
            SubscriptionData subscriptionData = objectMapper.convertValue(data, SubscriptionData.class);
            log.info("Processing subscription.disable for: {}",
                    sanitize.sanitizeLogging(subscriptionData.getSubscriptionCode()));

            subscriptionService.updateSubscriptionFromWebhook(subscriptionData);
        } catch (Exception e) {
            log.error("Failed to handle subscription.disable: {}", e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process subscription.disable", e);
        }
    }

    /**
     * Handle subscription not renewing
     */
    private void handleSubscriptionNotRenew(Map<String, Object> data) {
        try {
            SubscriptionData subscriptionData = objectMapper.convertValue(data, SubscriptionData.class);
            log.info("Processing subscription.not_renew for: {}",
                    sanitize.sanitizeLogging(subscriptionData.getSubscriptionCode()));

            subscriptionService.updateSubscriptionFromWebhook(subscriptionData);
        } catch (Exception e) {
            log.error("Failed to handle subscription.not_renew: {}", e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process subscription.not_renew", e);
        }
    }

    /**
     * Handle expiring cards notification
     */
    private void handleSubscriptionExpiringCards(Map<String, Object> data) {
        // Log for now - can be extended to send notifications to users
        log.info("Received subscription.expiring_cards notification: {}",
                sanitize.sanitizeLogging(String.valueOf(data)));
    }

    /**
     * Handle invoice creation
     */
    private void handleInvoiceCreate(Map<String, Object> data) {
        log.info("Processing invoice.create: {}", sanitize.sanitizeLogging(String.valueOf(data)));
        // Can be extended to create invoice records
    }

    /**
     * Handle invoice payment failed
     */
    private void handleInvoicePaymentFailed(Map<String, Object> data) {
        log.info("Processing invoice.payment_failed: {}", sanitize.sanitizeLogging(String.valueOf(data)));
        // Can be extended to handle failed invoice payments
    }

    /**
     * Handle invoice update
     */
    private void handleInvoiceUpdate(Map<String, Object> data) {
        log.info("Processing invoice.update: {}", sanitize.sanitizeLogging(String.valueOf(data)));
        // Can be extended to update invoice records
    }

    /**
     * Handle successful transfer
     */
    private void handleTransferSuccess(Map<String, Object> data) {
        log.info("Processing transfer.success: {}", sanitize.sanitizeLogging(String.valueOf(data)));
        // Can be extended for transfer handling
    }

    /**
     * Handle failed transfer
     */
    private void handleTransferFailed(Map<String, Object> data) {
        log.info("Processing transfer.failed: {}", sanitize.sanitizeLogging(String.valueOf(data)));
        // Can be extended for transfer handling
    }

    /**
     * Handle refund processed
     */
    private void handleRefundProcessed(Map<String, Object> data) {
        try {
            String reference = (String) data.get("transaction_reference");
            Object amountObj = data.get("amount");

            if (reference != null && amountObj != null) {
                long amountInKobo;
                if (amountObj instanceof Number) {
                    amountInKobo = ((Number) amountObj).longValue();
                } else {
                    amountInKobo = Long.parseLong(amountObj.toString());
                }

                BigDecimal refundAmount = BigDecimal.valueOf(amountInKobo)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                log.info(
                        "Processing refund.processed for reference: {}, amount: {}",
                        sanitize.sanitizeLogging(reference),
                        sanitize.sanitizeLogging(String.valueOf(refundAmount)));
                paymentService.recordRefund(reference, refundAmount);
            }
        } catch (Exception e) {
            log.error("Failed to handle refund.processed: {}", e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process refund.processed", e);
        }
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Generate receipt for Paystack payment
     */
    private void generateReceipt(PaystackPayment payment, TransactionData transactionData) {
        try {
            User user = payment.getUser();
            BigDecimal amount = payment.getAmount();

            String paymentMethodDetails = extractPaymentMethodDetails(transactionData);

            ReceiptData receiptData = ReceiptData.builder()
                    .userId(user.getId())
                    .customerName(user.getFirstName() + " " + user.getLastName())
                    .customerEmail(user.getEmail())
                    .paymentProvider(PaymentProvider.PAYSTACK)
                    .externalPaymentId(payment.getReference())
                    .amount(amount)
                    .currency(payment.getCurrency().toUpperCase())
                    .paymentMethod(payment.getChannel())
                    .paymentMethodDetails(paymentMethodDetails)
                    .description(payment.getDescription())
                    .paidAt(payment.getPaidAt() != null ? payment.getPaidAt() : OffsetDateTime.now())
                    .build();

            if (receiptEventPublisher != null) {
                receiptEventPublisher.publishReceiptRequest(receiptData);
            } else {
                log.debug("Receipt event publisher not available, skipping receipt generation for payment: {}",
                        sanitize.sanitizeLogging(payment.getReference()));
            }
        } catch (Exception e) {
            log.error("Failed to generate receipt for payment {}: {}", sanitize.sanitizeLogging(payment.getReference()),
                    e.getMessage());
            // Don't rethrow - receipt generation failure shouldn't fail the webhook
        }
    }

    /**
     * Extract payment method details from transaction data
     */
    private String extractPaymentMethodDetails(TransactionData transactionData) {
        try {
            if (transactionData.getAuthorization() != null) {
                var auth = transactionData.getAuthorization();
                String last4 = auth.getLast4();
                String brand = auth.getCardType();
                if (last4 != null && brand != null) {
                    return "**** " + last4 + " (" + brand.toUpperCase() + ")";
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract payment method details: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Record coupon usage if a coupon was applied to the payment.
     */
    private void recordCouponUsageIfApplicable(PaystackPayment payment) {
        String couponCode = payment.getCouponCode();
        if (couponCode == null || couponCode.isBlank()) {
            return;
        }

        try {
            log.info("Recording coupon usage for code: {} on payment: {}",
                    sanitize.sanitizeLogging(couponCode),
                    sanitize.sanitizeLogging(payment.getReference()));

            Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase())
                    .orElse(null);

            if (coupon == null) {
                log.warn("Coupon {} not found when trying to record usage for payment {}",
                        sanitize.sanitizeLogging(couponCode),
                        sanitize.sanitizeLogging(payment.getReference()));
                return;
            }

            User user = payment.getUser();
            BigDecimal originalAmount = payment.getOriginalAmount() != null
                    ? payment.getOriginalAmount()
                    : payment.getAmount();
            BigDecimal finalAmount = payment.getAmount();

            couponValidationService.recordCouponUsage(
                    coupon,
                    user,
                    originalAmount,
                    finalAmount,
                    payment.getReference(),
                    payment.getPlanCode());

            log.info("Successfully recorded coupon usage for {} on payment {}",
                    sanitize.sanitizeLogging(couponCode),
                    sanitize.sanitizeLogging(payment.getReference()));

        } catch (Exception e) {
            log.error("Failed to record coupon usage for payment {}: {}",
                    sanitize.sanitizeLogging(payment.getReference()),
                    e.getMessage());
            // Don't rethrow - coupon usage recording failure shouldn't fail the webhook
        }
    }
}
