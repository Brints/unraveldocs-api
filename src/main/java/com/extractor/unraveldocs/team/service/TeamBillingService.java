package com.extractor.unraveldocs.team.service;

import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import com.extractor.unraveldocs.team.model.Team;

import java.math.BigDecimal;

/**
 * Service interface for team billing operations.
 * Implementations handle Stripe and Paystack payment integrations.
 */
public interface TeamBillingService {

    /**
     * Charge a team's subscription using their configured payment gateway.
     *
     * @param team The team to charge
     * @return true if charge was successful, false otherwise
     */
    boolean chargeSubscription(Team team);

    /**
     * Create a subscription for a team in the payment gateway.
     *
     * @param team         The team to create subscription for
     * @param paymentToken Token from frontend for payment method
     * @return true if subscription was created successfully
     */
    boolean createSubscription(Team team, String paymentToken);

    /**
     * Cancel a team's subscription in the payment gateway.
     *
     * @param team The team to cancel subscription for
     * @return true if cancellation was successful
     */
    boolean cancelSubscription(Team team);

    /**
     * Calculate the price for a team subscription.
     *
     * @param team  The team
     * @param cycle The billing cycle
     * @return The price amount
     */
    BigDecimal calculatePrice(Team team, TeamBillingCycle cycle);
}
