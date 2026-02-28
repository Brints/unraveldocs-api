package com.extractor.unraveldocs.credit.controller;

import com.extractor.unraveldocs.credit.dto.request.PurchaseCreditPackRequest;
import com.extractor.unraveldocs.credit.dto.request.TransferCreditsRequest;
import com.extractor.unraveldocs.credit.dto.response.*;
import com.extractor.unraveldocs.credit.model.CreditTransaction;
import com.extractor.unraveldocs.credit.repository.CreditTransactionRepository;
import com.extractor.unraveldocs.credit.service.CreditBalanceService;
import com.extractor.unraveldocs.credit.service.CreditPackManagementService;
import com.extractor.unraveldocs.credit.service.CreditPurchaseService;
import com.extractor.unraveldocs.credit.service.PageCountService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * User-facing controller for credit pack operations.
 */
@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
@Tag(name = "Credit Pack", description = "Credit pack purchase and balance management")
public class CreditPackController {

        private final CreditPackManagementService packManagementService;
        private final CreditBalanceService creditBalanceService;
        private final CreditPurchaseService creditPurchaseService;
        private final PageCountService pageCountService;
        private final CreditTransactionRepository transactionRepository;
        private final ResponseBuilderService responseBuilderService;

        @Operation(
                summary = "List available credit packs",
                description = "Returns all active credit packs available for purchase. Use ?currency=NGN to see prices in local currency."
        )
        @GetMapping("/packs")
        public ResponseEntity<UnravelDocsResponse<List<CreditPackData>>> getAvailablePacks(
                        @RequestParam(required = false) String currency) {
                List<CreditPackData> packs;
                if (currency != null && !currency.isBlank() && !currency.equalsIgnoreCase("USD")) {
                        packs = packManagementService.getActivePacksWithCurrency(currency);
                } else {
                        packs = packManagementService.getActivePacks();
                }
                return ResponseEntity.ok(
                                responseBuilderService.buildUserResponse(
                                                packs,
                                                HttpStatus.OK,
                                                "Credit packs retrieved"));
        }

        @Operation(summary = "Get credit balance", description = "Returns the current user's credit balance")
        @GetMapping("/balance")
        public ResponseEntity<UnravelDocsResponse<CreditBalanceData>> getBalance(
                        @AuthenticationPrincipal User user) {
                CreditBalanceData balance = creditBalanceService.getBalanceData(user.getId());
                return ResponseEntity.ok(
                                responseBuilderService.buildUserResponse(
                                                balance,
                                                HttpStatus.OK,
                                                "Credit balance retrieved"));
        }

        @Operation(
                summary = "Purchase a credit pack",
                description = "Initializes a credit pack purchase with optional coupon code"
        )
        @PostMapping("/purchase")
        public ResponseEntity<UnravelDocsResponse<CreditPurchaseData>> purchasePack(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody PurchaseCreditPackRequest request) {
                CreditPurchaseData purchaseData = creditPurchaseService.initializePurchase(user, request);
                return ResponseEntity.ok(
                                responseBuilderService.buildUserResponse(purchaseData, HttpStatus.OK,
                                                "Payment initialized"));
        }

        @Operation(
                summary = "Transfer credits to another user",
                description = "Transfers credits to another user by email. Sender must retain at least 5 credits. Regular users are limited to 30 credits per month."
        )
        @PostMapping("/transfer")
        public ResponseEntity<UnravelDocsResponse<CreditTransferData>> transferCredits(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody TransferCreditsRequest request) {
                CreditTransferData transferData = creditBalanceService.transferCredits(
                                user, request.getRecipientEmail(), request.getAmount());
                return ResponseEntity.ok(
                                responseBuilderService.buildUserResponse(transferData, HttpStatus.OK,
                                                "Credits transferred successfully"));
        }

        @Operation(
                summary = "Get transaction history",
                description = "Returns paginated credit transaction history"
        )
        @GetMapping("/transactions")
        public ResponseEntity<UnravelDocsResponse<Page<CreditTransactionData>>> getTransactions(
                        @AuthenticationPrincipal User user,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                size = Math.min(size, 100); // Limit page size to 100
                Page<CreditTransactionData> transactions = transactionRepository
                                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                                .map(this::toTransactionData);
                return ResponseEntity.ok(
                                responseBuilderService.buildUserResponse(transactions, HttpStatus.OK,
                                                "Transactions retrieved"));
        }

        @Operation(
                summary = "Calculate credit cost",
                description = "Calculates how many credits are needed to process the uploaded files"
        )
        @PostMapping("/calculate")
        public ResponseEntity<UnravelDocsResponse<PageCountData>> calculateCost(
                        @AuthenticationPrincipal User user,
                        @RequestParam("files") MultipartFile[] files) {
                int totalPages = pageCountService.calculatePageCount(files);
                CreditBalanceData balance = creditBalanceService.getBalanceData(user.getId());

                PageCountData data = PageCountData.builder()
                                .totalPages(totalPages)
                                .creditsRequired(totalPages)
                                .currentBalance(balance.getBalance())
                                .hasEnoughCredits(balance.getBalance() >= totalPages)
                                .build();

                return ResponseEntity.ok(
                                responseBuilderService.buildUserResponse(data, HttpStatus.OK, "Page count calculated"));
        }

        private CreditTransactionData toTransactionData(CreditTransaction tx) {
                return CreditTransactionData.builder()
                                .transactionId(tx.getId())
                                .type(tx.getType().name())
                                .amount(tx.getAmount())
                                .balanceAfter(tx.getBalanceAfter())
                                .description(tx.getDescription())
                                .createdAt(tx.getCreatedAt())
                                .build();
        }
}
