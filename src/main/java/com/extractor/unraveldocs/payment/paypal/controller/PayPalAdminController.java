package com.extractor.unraveldocs.payment.paypal.controller;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.payment.paypal.service.PayPalPlanSetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing PayPal billing plans.
 * These endpoints should only be accessible by super admins.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/paypal")
@RequiredArgsConstructor
@Tag(name = "PayPal Admin", description = "Admin endpoints for PayPal plan management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PayPalAdminController {

    private final PayPalPlanSetupService planSetupService;
    private final SanitizeLogging sanitizer;

    /**
     * Set up all PayPal billing plans.
     * Creates a product and billing plans for each subscription tier.
     * This should only be run once during initial setup or when adding new plans.
     */
    @PostMapping("/plans/setup")
    @Operation(summary = "Set up PayPal billing plans", description = "Creates PayPal product and billing plans for all subscription tiers. "
            +
            "This should be run once during initial setup. " +
            "Skips FREE plan as it doesn't require PayPal subscription.")
    public ResponseEntity<Map<String, Object>> setupPlans() {
        log.info("Admin triggered PayPal plan setup");

        Map<String, Object> result = planSetupService.setupAllPlans();

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(Map.of(
                    "statusCode", 200,
                    "status", "success",
                    "message", "PayPal plans setup completed successfully",
                    "data", result));
        } else {
            return ResponseEntity.ok(Map.of(
                    "statusCode", 207,
                    "status", "partial",
                    "message", "PayPal plans setup completed with some errors",
                    "data", result));
        }
    }

    /**
     * List all existing PayPal billing plans.
     */
    @GetMapping("/plans")
    @Operation(summary = "List PayPal billing plans", description = "Lists all existing PayPal billing plans")
    public ResponseEntity<Map<String, Object>> listPlans() {
        log.info("Admin listing PayPal plans");

        List<Map<String, Object>> plans = planSetupService.listExistingPlans();

        return ResponseEntity.ok(Map.of(
                "statusCode", 200,
                "status", "success",
                "message", "PayPal plans retrieved successfully",
                "data", Map.of(
                        "plans", plans,
                        "count", plans.size())));
    }

    /**
     * Deactivate a PayPal billing plan.
     */
    @PostMapping("/plans/{planId}/deactivate")
    @Operation(summary = "Deactivate PayPal billing plan", description = "Deactivates a PayPal billing plan. Existing subscriptions will continue.")
    public ResponseEntity<Map<String, Object>> deactivatePlan(@PathVariable String planId) {
        log.info("Admin deactivating PayPal plan: {}", sanitizer.sanitizeLogging(planId));

        planSetupService.deactivatePlan(planId);

        return ResponseEntity.ok(Map.of(
                "statusCode", 200,
                "status", "success",
                "message", "PayPal plan deactivated successfully",
                "data", Map.of("planId", planId)));
    }
}
