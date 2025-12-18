package com.extractor.unraveldocs.payment.stripe.service;

import com.extractor.unraveldocs.payment.enums.PaymentStatus;
import com.extractor.unraveldocs.payment.enums.PaymentType;
import com.extractor.unraveldocs.payment.stripe.model.StripeCustomer;
import com.extractor.unraveldocs.payment.stripe.model.StripePayment;
import com.extractor.unraveldocs.payment.stripe.repository.StripePaymentRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for tracking and managing Stripe payments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentService {

    private final StripePaymentRepository stripePaymentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Record a new payment
     */
    @Transactional
    public StripePayment recordPayment(User user, StripeCustomer customer, PaymentType paymentType,
                                       String paymentIntentId, String subscriptionId, String invoiceId,
                                       String checkoutSessionId, BigDecimal amount, String currency,
                                       PaymentStatus status, String paymentMethodId, String description,
                                       Map<String, String> metadata) {
        StripePayment payment = new StripePayment();
        payment.setUser(user);
        payment.setStripeCustomer(customer);
        payment.setPaymentType(paymentType);
        payment.setPaymentIntentId(paymentIntentId);
        payment.setSubscriptionId(subscriptionId);
        payment.setInvoiceId(invoiceId);
        payment.setCheckoutSessionId(checkoutSessionId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(status);
        payment.setPaymentMethodId(paymentMethodId);
        payment.setDescription(description);
        
        if (metadata != null && !metadata.isEmpty()) {
            try {
                payment.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize payment metadata: {}", e.getMessage());
            }
        }

        StripePayment savedPayment = stripePaymentRepository.save(payment);
        log.info("Recorded payment {} with status {} for user {}", savedPayment.getId(), status, user.getId());

        return savedPayment;
    }

    /**
     * Update payment status
     */
    @Transactional
    public Optional<StripePayment> updatePaymentStatus(String paymentIntentId, PaymentStatus status, 
                                                       String failureMessage, String receiptUrl) {
        Optional<StripePayment> paymentOpt = stripePaymentRepository.findByPaymentIntentId(paymentIntentId);
        
        paymentOpt.ifPresent(payment -> {
            payment.setStatus(status);
            if (failureMessage != null) {
                payment.setFailureMessage(failureMessage);
            }
            if (receiptUrl != null) {
                payment.setReceiptUrl(receiptUrl);
            }
            stripePaymentRepository.save(payment);
            log.info("Updated payment {} status to {}", payment.getId(), status);
        });
        
        return paymentOpt;
    }

    /**
     * Record a refund
     */
    @Transactional
    public void recordRefund(String paymentIntentId, BigDecimal refundAmount) {
        stripePaymentRepository.findByPaymentIntentId(paymentIntentId).ifPresent(payment -> {
            BigDecimal currentRefunded = payment.getAmountRefunded() != null ? payment.getAmountRefunded() : BigDecimal.ZERO;
            payment.setAmountRefunded(currentRefunded.add(refundAmount));
            
            // Update status based on refund amount
            if (payment.getAmountRefunded().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            
            stripePaymentRepository.save(payment);
            log.info("Recorded refund of {} for payment {}", refundAmount, payment.getId());
        });
    }

    /**
     * Get payment by payment intent ID
     */
    public Optional<StripePayment> getPaymentByIntentId(String paymentIntentId) {
        return stripePaymentRepository.findByPaymentIntentId(paymentIntentId);
    }

    /**
     * Get payment by checkout session ID
     */
    public Optional<StripePayment> getPaymentBySessionId(String checkoutSessionId) {
        return stripePaymentRepository.findByCheckoutSessionId(checkoutSessionId);
    }

    /**
     * Get all payments for a user
     */
    public Page<StripePayment> getUserPayments(String userId, Pageable pageable) {
        return stripePaymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get payments by subscription ID
     */
    public List<StripePayment> getPaymentsBySubscription(String subscriptionId) {
        return stripePaymentRepository.findBySubscriptionId(subscriptionId);
    }

    /**
     * Get payments within date range
     */
    public List<StripePayment> getPaymentsByDateRange(String userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return stripePaymentRepository.findPaymentsByUserAndDateRange(userId, startDate, endDate);
    }

    /**
     * Count successful payments for user
     */
    public long countSuccessfulPayments(String userId) {
        return stripePaymentRepository.countByUserIdAndStatus(userId, PaymentStatus.SUCCEEDED);
    }

    /**
     * Check if payment exists
     */
    public boolean paymentExists(String paymentIntentId) {
        return stripePaymentRepository.existsByPaymentIntentId(paymentIntentId);
    }
}
