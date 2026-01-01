package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.enums.PaymentStatus;
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
    private final ObjectMapper objectMapper;
    private final SanitizeLogging sanitize;

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

                // Generate receipt
                generateReceipt(payment, transactionData);
            });
        } catch (Exception e) {
            log.error("Failed to handle charge.success: {}", e.getMessage(), e);
            throw new PaystackWebhookException("Failed to process charge.success", e);
        }
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
}
