package com.extractor.unraveldocs.subscription.controller;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.extractor.unraveldocs.subscription.dto.response.AllSubscriptionPlans;
import com.extractor.unraveldocs.subscription.dto.response.SubscriptionPlansData;
import com.extractor.unraveldocs.subscription.service.SubscriptionPlansService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPER_ADMIN'))")
@Tag(name = "Subscription Management", description = "APIs for managing subscription plans and assigning them to users.")
public class SubscriptionController {
    private final SubscriptionPlansService subscriptionPlansService;

    @Operation(
            summary = "Create a new subscription plan",
            description = "Allows an admin to create a new subscription plan.",
            responses = {
                   @ApiResponse(responseCode = "201", description = "Subscription plan created successfully",
                   content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SubscriptionPlansData.class))),
                   @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized"),
                   @ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to create subscription plan")
            }
    )
    @PostMapping("plans")
    public ResponseEntity<UnravelDocsDataResponse<SubscriptionPlansData>> createSubscriptionPlan(CreateSubscriptionPlanRequest request) {
        UnravelDocsDataResponse<SubscriptionPlansData> createdPlan = subscriptionPlansService.createSubscriptionPlan(request);

        return new ResponseEntity<>(createdPlan, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update an existing subscription plan",
            description = "Allows an admin to update an existing subscription plan.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Subscription plan updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SubscriptionPlansData.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Subscription plan not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to update subscription plan")
            }
    )
    @PutMapping(value = "plans/{planId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UnravelDocsDataResponse<SubscriptionPlansData>> updateSubscriptionPlan(
            @RequestBody UpdateSubscriptionPlanRequest request, @PathVariable String planId) {

        UnravelDocsDataResponse<SubscriptionPlansData> updatedPlan =
                subscriptionPlansService.updateSubscriptionPlan(planId, request);

        return new ResponseEntity<>(updatedPlan, HttpStatus.OK);
    }

    @Operation(
            summary = "Assign subscriptions to all existing users without one",
            description = "Assigns a default subscription to all users who do not have a subscription.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Subscriptions assigned successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to assign subscriptions")
            }
    )
    @PostMapping("assign-subscriptions-to-existing-users")
    public ResponseEntity<UnravelDocsDataResponse<AllSubscriptionPlans>> assignToExistingUsers() {
        UnravelDocsDataResponse<AllSubscriptionPlans> response =
                subscriptionPlansService.assignSubscriptionsToExistingUsers();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
