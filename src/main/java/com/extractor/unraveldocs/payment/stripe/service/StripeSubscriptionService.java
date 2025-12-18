package com.extractor.unraveldocs.payment.stripe.service;

import com.extractor.unraveldocs.payment.enums.SubscriptionStatus;
import com.extractor.unraveldocs.payment.stripe.model.StripeCustomer;
import com.extractor.unraveldocs.payment.stripe.model.StripeSubscription;
import com.extractor.unraveldocs.payment.stripe.repository.StripeSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for tracking and managing Stripe subscriptions locally.
 * This service maintains a local copy of subscription data to enable
 * faster queries and reduce Stripe API calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeSubscriptionService {

    private final StripeSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Record or update a subscription from a Stripe subscription object
     */
    @Transactional
    public StripeSubscription recordSubscription(User user, StripeCustomer customer, Subscription stripeSubscription) {
        StripeSubscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscription.getId())
                .orElseGet(() -> {
                    StripeSubscription newSub = new StripeSubscription();
                    newSub.setUser(user);
                    newSub.setStripeCustomer(customer);
                    newSub.setStripeSubscriptionId(stripeSubscription.getId());
                    return newSub;
                });

        // Update fields from Stripe subscription
        updateSubscriptionFromStripe(subscription, stripeSubscription);

        StripeSubscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Recorded subscription {} with status {} for user {}",
                savedSubscription.getStripeSubscriptionId(),
                savedSubscription.getStatus(),
                user.getId());

        return savedSubscription;
    }

    /**
     * Update subscription status
     */
    @Transactional
    public Optional<StripeSubscription> updateSubscriptionStatus(String stripeSubscriptionId, SubscriptionStatus status) {
        return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .map(subscription -> {
                    subscription.setStatus(status);
                    StripeSubscription saved = subscriptionRepository.save(subscription);
                    log.info("Updated subscription {} status to {}", stripeSubscriptionId, status);
                    return saved;
                });
    }

    /**
     * Update subscription from Stripe subscription object
     */
    @Transactional
    public Optional<StripeSubscription> updateFromStripe(Subscription stripeSubscription) {
        return subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId())
                .map(subscription -> {
                    updateSubscriptionFromStripe(subscription, stripeSubscription);
                    return subscriptionRepository.save(subscription);
                });
    }

    /**
     * Mark subscription as canceled
     */
    @Transactional
    public Optional<StripeSubscription> cancelSubscription(String stripeSubscriptionId, boolean immediately) {
        return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .map(subscription -> {
                    if (immediately) {
                        subscription.setStatus(SubscriptionStatus.CANCELED);
                        subscription.setCanceledAt(OffsetDateTime.now());
                    } else {
                        subscription.setCancelAtPeriodEnd(true);
                    }
                    StripeSubscription saved = subscriptionRepository.save(subscription);
                    log.info("Canceled subscription {} (immediately: {})", stripeSubscriptionId, immediately);
                    return saved;
                });
    }

    /**
     * Get subscription by Stripe subscription ID
     */
    public Optional<StripeSubscription> getByStripeSubscriptionId(String stripeSubscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    }

    /**
     * Get active subscription for user
     */
    public Optional<StripeSubscription> getActiveSubscription(String userId) {
        return subscriptionRepository.findActiveByUserId(userId);
    }

    /**
     * Get all subscriptions for user
     */
    public Page<StripeSubscription> getUserSubscriptions(String userId, Pageable pageable) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get all active subscriptions for user
     */
    public List<StripeSubscription> getActiveSubscriptions(String userId) {
        return subscriptionRepository.findAllActiveByUserId(userId);
    }

    /**
     * Check if user has active subscription
     */
    public boolean hasActiveSubscription(String userId) {
        return subscriptionRepository.countActiveByUserId(userId) > 0;
    }

    /**
     * Get subscriptions pending cancellation
     */
    public List<StripeSubscription> getSubscriptionsPendingCancellation() {
        return subscriptionRepository.findByCancelAtPeriodEndTrue();
    }

    /**
     * Get subscriptions expiring within specified hours
     */
    public List<StripeSubscription> getExpiringSubscriptions(int hoursFromNow) {
        OffsetDateTime expirationDate = OffsetDateTime.now().plusHours(hoursFromNow);
        return subscriptionRepository.findExpiringBefore(expirationDate);
    }

    /**
     * Check if subscription exists
     */
    public boolean subscriptionExists(String stripeSubscriptionId) {
        return subscriptionRepository.existsByStripeSubscriptionId(stripeSubscriptionId);
    }

    /**
     * Helper method to update local subscription from Stripe object
     */
    private void updateSubscriptionFromStripe(StripeSubscription subscription, Subscription stripeSubscription) {
        subscription.setStatus(mapStripeStatus(stripeSubscription.getStatus()));
        subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());
        subscription.setDefaultPaymentMethodId(stripeSubscription.getDefaultPaymentMethod());
        subscription.setLatestInvoiceId(stripeSubscription.getLatestInvoice());
        subscription.setCurrency(stripeSubscription.getCurrency());

        // Set period dates from subscription items
        if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
            SubscriptionItem firstItem = stripeSubscription.getItems().getData().getFirst();
            subscription.setCurrentPeriodStart(convertToOffsetDateTime(firstItem.getCurrentPeriodStart()));
            subscription.setCurrentPeriodEnd(convertToOffsetDateTime(firstItem.getCurrentPeriodEnd()));

            // Set price and product info
            if (firstItem.getPrice() != null) {
                subscription.setPriceId(firstItem.getPrice().getId());
                subscription.setProductId(firstItem.getPrice().getProduct());
            }
            subscription.setQuantity(firstItem.getQuantity());
        }

        // Set trial dates
        if (stripeSubscription.getTrialStart() != null) {
            subscription.setTrialStart(convertToOffsetDateTime(stripeSubscription.getTrialStart()));
        }
        if (stripeSubscription.getTrialEnd() != null) {
            subscription.setTrialEnd(convertToOffsetDateTime(stripeSubscription.getTrialEnd()));
        }

        // Set cancellation dates
        if (stripeSubscription.getCancelAt() != null) {
            subscription.setCancelAt(convertToOffsetDateTime(stripeSubscription.getCancelAt()));
        }
        if (stripeSubscription.getCanceledAt() != null) {
            subscription.setCanceledAt(convertToOffsetDateTime(stripeSubscription.getCanceledAt()));
        }

        // Set metadata
        Map<String, String> metadata = stripeSubscription.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
            try {
                subscription.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize subscription metadata: {}", e.getMessage());
            }
        }
    }

    /**
     * Map Stripe subscription status to local enum
     */
    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus.toLowerCase()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "unpaid" -> SubscriptionStatus.UNPAID;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> SubscriptionStatus.INCOMPLETE_EXPIRED;
            case "paused" -> SubscriptionStatus.PAUSED;
            default -> SubscriptionStatus.ACTIVE;
        };
    }

    /**
     * Convert Unix timestamp to OffsetDateTime
     */
    private OffsetDateTime convertToOffsetDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.UTC);
    }
}
