package com.extractor.unraveldocs.credit.controller;

import com.extractor.unraveldocs.credit.dto.request.AllocateCreditsRequest;
import com.extractor.unraveldocs.credit.dto.request.CreateCreditPackRequest;
import com.extractor.unraveldocs.credit.dto.request.UpdateCreditPackRequest;
import com.extractor.unraveldocs.credit.dto.response.CreditPackData;
import com.extractor.unraveldocs.credit.service.CreditBalanceService;
import com.extractor.unraveldocs.credit.service.CreditPackManagementService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for managing credit packs.
 * Requires ADMIN or SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin/credits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Credit Pack Management", description = "Admin endpoints for managing credit pack configurations and allocations")
public class AdminCreditPackController {

    private final CreditPackManagementService packManagementService;
    private final CreditBalanceService creditBalanceService;
    private final ResponseBuilderService responseBuilderService;

    @Operation(summary = "Create a new credit pack", description = "Creates a new credit pack with specified price and credit amount")
    @PostMapping("/packs")
    public ResponseEntity<UnravelDocsResponse<CreditPackData>> createPack(
            @Valid @RequestBody CreateCreditPackRequest request) {
        CreditPackData pack = packManagementService.createPack(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseBuilderService.buildUserResponse(pack, HttpStatus.CREATED, "Credit pack created"));
    }

    @Operation(summary = "Update a credit pack", description = "Updates price, credit amount, or active status of a credit pack")
    @PutMapping("/packs/{id}")
    public ResponseEntity<UnravelDocsResponse<CreditPackData>> updatePack(
            @PathVariable String id,
            @Valid @RequestBody UpdateCreditPackRequest request) {
        CreditPackData pack = packManagementService.updatePack(id, request);
        return ResponseEntity.ok(
                responseBuilderService.buildUserResponse(pack, HttpStatus.OK, "Credit pack updated"));
    }

    @Operation(summary = "Deactivate a credit pack", description = "Soft-deletes a credit pack by marking it inactive")
    @DeleteMapping("/packs/{id}")
    public ResponseEntity<UnravelDocsResponse<Void>> deactivatePack(@PathVariable String id) {
        packManagementService.deactivatePack(id);
        return ResponseEntity.ok(
                responseBuilderService.buildVoidResponse(HttpStatus.OK, "Credit pack deactivated"));
    }

    @Operation(summary = "List all credit packs (admin)", description = "Returns all packs including inactive ones for admin management")
    @GetMapping("/packs")
    public ResponseEntity<UnravelDocsResponse<List<CreditPackData>>> getAllPacks() {
        List<CreditPackData> packs = packManagementService.getAllPacks();
        return ResponseEntity.ok(
                responseBuilderService.buildUserResponse(packs, HttpStatus.OK, "All credit packs retrieved"));
    }

    @Operation(summary = "Get credit pack by ID", description = "Returns details of a specific credit pack")
    @GetMapping("/packs/{id}")
    public ResponseEntity<UnravelDocsResponse<CreditPackData>> getPackById(@PathVariable String id) {
        CreditPackData pack = packManagementService.getPackById(id);
        return ResponseEntity.ok(
                responseBuilderService.buildUserResponse(pack, HttpStatus.OK, "Credit pack retrieved"));
    }

    @Operation(summary = "Allocate credits to a user", description = "Allocates credits to any user without restrictions. Only available to ADMIN and SUPER_ADMIN.")
    @PostMapping("/allocate")
    public ResponseEntity<UnravelDocsResponse<Void>> allocateCredits(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody AllocateCreditsRequest request) {
        creditBalanceService.adminAllocateCredits(admin, request.getUserId(), request.getAmount(), request.getReason());
        return ResponseEntity.ok(
                responseBuilderService.buildVoidResponse(HttpStatus.OK,
                        "Successfully allocated " + request.getAmount() + " credits to user"));
    }
}
