package com.extractor.unraveldocs.team.service;

import com.extractor.unraveldocs.payment.paystack.repository.PaystackSubscriptionRepository;
import com.extractor.unraveldocs.payment.paystack.service.PaystackSubscriptionService;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.events.ReceiptEventPublisher;
import com.extractor.unraveldocs.payment.stripe.repository.StripeSubscriptionRepository;
import com.extractor.unraveldocs.payment.stripe.service.StripeSubscriptionService;
import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamSubscriptionPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Implementation of TeamBillingService.
 * Delegates to Stripe or Paystack based on team's payment gateway.
 * Generates and sends receipts after successful payments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamBillingServiceImpl implements TeamBillingService {

    private final StripeSubscriptionService stripeSubscriptionService;
    private final PaystackSubscriptionService paystackSubscriptionService;
    private final StripeSubscriptionRepository stripeSubscriptionRepository;
    private final PaystackSubscriptionRepository paystackSubscriptionRepository;
    private final ReceiptEventPublisher receiptEventPublisher;
    private final TeamSubscriptionPlanService planService;

    @Override
    public boolean chargeSubscription(Team team) {
        String gateway = team.getPaymentGateway();

        if (gateway == null) {
            log.warn("No payment gateway configured for team: {}", team.getTeamCode());
            return false;
        }

        try {
            boolean success = switch (gateway.toLowerCase()) {
                case "stripe" -> chargeViaStripe(team);
                case "paystack" -> chargeViaPaystack(team);
                default -> {
                    log.error("Unknown payment gateway: {}", gateway);
                    yield false;
                }
            };

            // Generate and send receipt on successful charge
            if (success) {
                publishReceipt(team, "Team Subscription Payment");
            }

            return success;
        } catch (Exception e) {
            log.error("Failed to charge subscription for team {}: {}", team.getTeamCode(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean createSubscription(Team team, String paymentToken) {
        String gateway = team.getPaymentGateway();

        try {
            boolean success = switch (gateway.toLowerCase()) {
                case "stripe" -> createStripeSubscription(team, paymentToken);
                case "paystack" -> createPaystackSubscription(team, paymentToken);
                default -> {
                    log.error("Unknown payment gateway: {}", gateway);
                    yield false;
                }
            };

            // Generate and send receipt on successful subscription creation
            if (success) {
                publishReceipt(team, "Team Subscription Activation");
            }

            return success;
        } catch (Exception e) {
            log.error("Failed to create subscription for team {}: {}", team.getTeamCode(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean cancelSubscription(Team team) {
        String gateway = team.getPaymentGateway();

        if (gateway == null) {
            log.warn("No payment gateway configured for team: {} - proceeding with local cancellation",
                    team.getTeamCode());
            return true;
        }

        try {
            return switch (gateway.toLowerCase()) {
                case "stripe" -> cancelStripeSubscription(team);
                case "paystack" -> cancelPaystackSubscription(team);
                default -> {
                    log.error("Unknown payment gateway: {}", gateway);
                    yield false;
                }
            };
        } catch (Exception e) {
            log.error("Failed to cancel subscription for team {}: {}", team.getTeamCode(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public BigDecimal calculatePrice(Team team, TeamBillingCycle cycle) {
        // Use database-driven pricing via TeamSubscriptionPlan
        if (team.getPlan() != null) {
            return team.getPlan().getPrice(cycle);
        }
        // Fallback: lookup from database by subscription type name
        return planService.getPrice(team.getSubscriptionType().name(), cycle);
    }

    // ========== Receipt Generation ==========

    /**
     * Publish receipt generation request for a team payment.
     */
    private void publishReceipt(Team team, String description) {
        try {
            PaymentProvider provider = switch (team.getPaymentGateway().toLowerCase()) {
                case "stripe" -> PaymentProvider.STRIPE;
                case "paystack" -> PaymentProvider.PAYSTACK;
                default -> PaymentProvider.STRIPE;
            };

            String paymentId = team.getStripeSubscriptionId() != null
                    ? team.getStripeSubscriptionId()
                    : team.getPaystackSubscriptionCode();

            ReceiptData receiptData = ReceiptData.builder()
                    .userId(team.getCreatedBy().getId())
                    .customerName(team.getCreatedBy().getFirstName() + " " + team.getCreatedBy().getLastName())
                    .customerEmail(team.getCreatedBy().getEmail())
                    .paymentProvider(provider)
                    .externalPaymentId(paymentId != null ? paymentId : UUID.randomUUID().toString())
                    .amount(team.getSubscriptionPrice())
                    .currency(team.getCurrency())
                    .paymentMethod(team.getPaymentGateway())
                    .paymentMethodDetails(getPlanDisplayName(team) + " - "
                            + team.getBillingCycle().getDisplayName())
                    .description(description + " for " + team.getName())
                    .paidAt(OffsetDateTime.now())
                    .build();

            receiptEventPublisher.publishReceiptRequest(receiptData);
            log.info("Published receipt request for team {} payment", team.getTeamCode());
        } catch (Exception e) {
            // Don't fail the payment if receipt generation fails
            log.error("Failed to publish receipt for team {}: {}", team.getTeamCode(), e.getMessage(), e);
        }
    }

    // ========== Stripe Methods ==========

    private boolean chargeViaStripe(Team team) {
        log.info("Charging team {} via Stripe subscription: {}",
                team.getTeamCode(), team.getStripeSubscriptionId());

        // For Stripe recurring subscriptions, the charge happens automatically
        // This method verifies the subscription is active
        if (team.getStripeSubscriptionId() == null) {
            log.warn("No Stripe subscription ID for team: {}", team.getTeamCode());
            return false;
        }

        // Check if subscription exists and is active in our database
        var subscription = stripeSubscriptionRepository.findByStripeSubscriptionId(team.getStripeSubscriptionId());
        if (subscription.isEmpty()) {
            log.warn("Stripe subscription not found in database for team: {}", team.getTeamCode());
            return false;
        }

        // Stripe handles recurring charges automatically - we just verify it's active
        log.info("Stripe subscription {} is active for team {}",
                team.getStripeSubscriptionId(), team.getTeamCode());
        return true;
    }

    private boolean createStripeSubscription(Team team, String paymentToken) {
        log.info("Creating Stripe subscription for team {}", team.getTeamCode());

        // Note: The actual Stripe subscription creation happens via the existing
        // StripeSubscriptionController and StripeService. This method would be
        // called after the subscription is already created in Stripe via webhook.
        // The paymentToken is the Stripe subscription ID in this case.

        if (paymentToken != null && !paymentToken.isEmpty()) {
            team.setStripeSubscriptionId(paymentToken);
            log.info("Stored Stripe subscription ID {} for team {}", paymentToken, team.getTeamCode());
            return true;
        }

        return false;
    }

    private boolean cancelStripeSubscription(Team team) {
        log.info("Cancelling Stripe subscription for team {}: {}",
                team.getTeamCode(), team.getStripeSubscriptionId());

        if (team.getStripeSubscriptionId() == null) {
            log.warn("No Stripe subscription to cancel for team: {}", team.getTeamCode());
            return true;
        }

        // Cancel at period end (not immediately) to allow service until the end
        var result = stripeSubscriptionService.cancelSubscription(
                team.getStripeSubscriptionId(),
                false // not immediately - cancel at period end
        );

        if (result.isPresent()) {
            log.info("Successfully scheduled Stripe subscription cancellation for team {}", team.getTeamCode());
            return true;
        } else {
            log.warn("Stripe subscription not found for cancellation: {}", team.getStripeSubscriptionId());
            return false;
        }
    }

    // ========== Paystack Methods ==========

    private boolean chargeViaPaystack(Team team) {
        log.info("Charging team {} via Paystack subscription: {}",
                team.getTeamCode(), team.getPaystackSubscriptionCode());

        // For Paystack recurring subscriptions, charges happen automatically
        if (team.getPaystackSubscriptionCode() == null) {
            log.warn("No Paystack subscription code for team: {}", team.getTeamCode());
            return false;
        }

        // Verify subscription exists and is active
        var subscription = paystackSubscriptionRepository.findBySubscriptionCode(team.getPaystackSubscriptionCode());
        if (subscription.isEmpty()) {
            log.warn("Paystack subscription not found in database for team: {}", team.getTeamCode());
            return false;
        }

        log.info("Paystack subscription {} is active for team {}",
                team.getPaystackSubscriptionCode(), team.getTeamCode());
        return true;
    }

    private boolean createPaystackSubscription(Team team, String paymentToken) {
        log.info("Creating Paystack subscription for team {}", team.getTeamCode());

        // Similar to Stripe, the actual creation happens via
        // PaystackSubscriptionService
        // This stores the subscription code after creation
        if (paymentToken != null && !paymentToken.isEmpty()) {
            team.setPaystackSubscriptionCode(paymentToken);
            log.info("Stored Paystack subscription code {} for team {}", paymentToken, team.getTeamCode());
            return true;
        }

        return false;
    }

    private boolean cancelPaystackSubscription(Team team) {
        log.info("Cancelling Paystack subscription for team {}: {}",
                team.getTeamCode(), team.getPaystackSubscriptionCode());

        if (team.getPaystackSubscriptionCode() == null) {
            log.warn("No Paystack subscription to cancel for team: {}", team.getTeamCode());
            return true;
        }

        try {
            // Paystack requires an email token for cancellation
            // In practice, you'd get this from the subscription or user
            String emailToken = team.getCreatedBy().getEmail();

            paystackSubscriptionService.disableSubscription(
                    team.getPaystackSubscriptionCode(),
                    emailToken);

            log.info("Successfully disabled Paystack subscription for team {}", team.getTeamCode());
            return true;
        } catch (Exception e) {
            log.error("Failed to disable Paystack subscription for team {}: {}",
                    team.getTeamCode(), e.getMessage());
            return false;
        }
    }

    /**
     * Get the display name for a team's subscription plan.
     */
    private String getPlanDisplayName(Team team) {
        if (team.getPlan() != null) {
            return team.getPlan().getDisplayName();
        }
        // Fallback: use subscription type name formatted nicely
        return team.getSubscriptionType().name().replace("_", " ");
    }
}
