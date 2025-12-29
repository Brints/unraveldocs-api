package com.extractor.unraveldocs.team.dto.request;

import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Request DTO for creating a new team.
 */
@Builder
public record CreateTeamRequest(
        @NotBlank(message = "Team name is required") @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters") String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters") String description,

        @NotNull(message = "Subscription type is required") TeamSubscriptionType subscriptionType,

        @NotNull(message = "Billing cycle is required") TeamBillingCycle billingCycle,

        @NotBlank(message = "Payment gateway is required") String paymentGateway,

        String paymentToken) {
}
