package com.extractor.unraveldocs.team.service;

import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import com.extractor.unraveldocs.team.model.TeamSubscriptionPlan;
import com.extractor.unraveldocs.team.repository.TeamSubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for managing team subscription plans.
 * Provides pricing and plan lookups from the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamSubscriptionPlanService {

    private final TeamSubscriptionPlanRepository planRepository;

    /**
     * Get the price for a plan based on billing cycle.
     */
    @Cacheable(value = "teamPlanPrices", key = "#planName + '-' + #cycle.name()")
    public BigDecimal getPrice(String planName, TeamBillingCycle cycle) {
        TeamSubscriptionPlan plan = getPlanByName(planName);
        return plan.getPrice(cycle);
    }

    /**
     * Get a plan by name.
     * Note: Not cached to avoid Redis deserialization issues with entity objects.
     * The database lookup is fast and this method is not called frequently.
     */
    public TeamSubscriptionPlan getPlanByName(String planName) {
        return planRepository.findActiveByName(planName)
                .orElseThrow(() -> new NotFoundException("Team subscription plan not found: " + planName));
    }

    /**
     * Get all active plans.
     * Note: Not cached to avoid Redis deserialization issues with entity objects.
     */
    public List<TeamSubscriptionPlan> getAllActivePlans() {
        return planRepository.findAllActive();
    }

    /**
     * Get the Stripe price ID for a plan.
     */
    public String getStripePriceId(String planName, TeamBillingCycle cycle) {
        TeamSubscriptionPlan plan = getPlanByName(planName);
        return plan.getStripePriceId(cycle);
    }

    /**
     * Get the Paystack plan code for a plan.
     */
    public String getPaystackPlanCode(String planName, TeamBillingCycle cycle) {
        TeamSubscriptionPlan plan = getPlanByName(planName);
        return plan.getPaystackPlanCode(cycle);
    }

    /**
     * Check if a plan exists.
     */
    public boolean planExists(String planName) {
        return planRepository.existsByName(planName);
    }
}
