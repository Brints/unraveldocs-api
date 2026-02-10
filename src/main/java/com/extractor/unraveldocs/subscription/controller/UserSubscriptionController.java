package com.extractor.unraveldocs.subscription.controller;

import com.extractor.unraveldocs.security.CurrentUser;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.dto.response.UserSubscriptionDetailsDto;
import com.extractor.unraveldocs.subscription.service.UserSubscriptionService;
import com.extractor.unraveldocs.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user subscription operations.
 * Provides endpoints for users to view their subscription details.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "User Subscriptions", description = "Endpoints for managing user subscriptions")
public class UserSubscriptionController {

        private final UserSubscriptionService userSubscriptionService;

        /**
         * Get the current user's subscription details.
         * Returns plan information, billing period, trial status, and usage.
         */
        @GetMapping("/me")
        @Operation(summary = "Get current user's subscription", description = "Returns detailed subscription information including plan details, "
                        +
                        "billing period dates, trial status, and usage statistics")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Subscription details retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated")
        })
        public ResponseEntity<UnravelDocsResponse<UserSubscriptionDetailsDto>> getMySubscription(
                        @CurrentUser User user) {

                UserSubscriptionDetailsDto subscriptionDetails = userSubscriptionService
                                .getUserSubscriptionDetails(user);

                UnravelDocsResponse<UserSubscriptionDetailsDto> response = new UnravelDocsResponse<>();
                response.setStatusCode(HttpStatus.OK.value());
                response.setStatus("success");
                response.setMessage("Subscription details retrieved successfully");
                response.setData(subscriptionDetails);

                return ResponseEntity.ok(response);
        }

        /**
         * Activate a free trial for a specific plan.
         */
        @PostMapping("/trial/{planId}")
        @Operation(summary = "Activate a free trial", description = "Activates a free trial for the specified plan if eligible")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Trial activated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid plan or trial not supported"),
                        @ApiResponse(responseCode = "409", description = "Trial already used or active subscription exists")
        })
        public ResponseEntity<UnravelDocsResponse<Void>> activateTrial(
                        @CurrentUser User user,
                        @PathVariable String planId) {

                userSubscriptionService.activateTrial(user, planId);

                UnravelDocsResponse<Void> response = new UnravelDocsResponse<>();
                response.setStatusCode(HttpStatus.OK.value());
                response.setStatus("success");
                response.setMessage("Trial activated successfully");

                return ResponseEntity.ok(response);
        }
}
