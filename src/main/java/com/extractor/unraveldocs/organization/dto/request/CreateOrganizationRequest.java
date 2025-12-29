package com.extractor.unraveldocs.organization.dto.request;

import com.extractor.unraveldocs.organization.datamodel.OrganizationSubscriptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequest {
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Subscription type is required")
    private OrganizationSubscriptionType subscriptionType;

    @NotBlank(message = "Payment gateway is required")
    private String paymentGateway;

    private String paymentToken;
}
