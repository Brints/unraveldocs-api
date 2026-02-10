package com.extractor.unraveldocs.subscription.service;

import com.extractor.unraveldocs.subscription.dto.response.UserSubscriptionDetailsDto;
import com.extractor.unraveldocs.user.model.User;

/**
 * Service interface for user subscription operations.
 */
public interface UserSubscriptionService {

    /**
     * Get the subscription details for the current user.
     * 
     * @param user the authenticated user
     * @return the user's subscription details
     */
    UserSubscriptionDetailsDto getUserSubscriptionDetails(User user);

    /**
     * Activate a trial for a specific plan for the user.
     * 
     * @param user   the authenticated user
     * @param planId the ID of the plan to try
     */
    void activateTrial(User user, String planId);

    /**
     * Check for expired trials and reset them to the free plan.
     * Should be called by a scheduled job.
     */
    void checkAndExpireTrials();

    /**
     * Validate if a user is eligible to start a new subscription or trial.
     * Throws an exception if the user already has an active paid subscription.
     * 
     * @param user the authenticated user
     * @throws IllegalStateException if eligibility validation fails
     */
    void validateSubscriptionEligibility(User user);
}
