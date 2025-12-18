package com.extractor.unraveldocs.payment.receipt.controller;

import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.exceptions.custom.UnauthorizedException;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptResponseDto;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Receipt management endpoints")
public class ReceiptController {

    private final ReceiptRepository receiptRepository;
    private final ResponseBuilderService responseBuilderService;

    @Operation(summary = "Get user's receipts", description = "Retrieve paginated list of receipts for the authenticated user")
    @GetMapping
    public ResponseEntity<UnravelDocsResponse<List<ReceiptResponseDto>>> getUserReceipts(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Receipt> receiptsPage = receiptRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<ReceiptResponseDto> receipts = receiptsPage.getContent().stream()
                .map(this::toResponseDto)
                .toList();

        UnravelDocsResponse<List<ReceiptResponseDto>> response = responseBuilderService.buildUserResponse(
                receipts,
                HttpStatus.OK,
                "Receipts retrieved successfully"
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get receipt by number", description = "Retrieve a specific receipt by its receipt number")
    @GetMapping("/{receiptNumber}")
    public ResponseEntity<UnravelDocsResponse<ReceiptResponseDto>> getReceiptByNumber(
            @AuthenticationPrincipal User user,
            @PathVariable String receiptNumber) {

        Receipt receipt = receiptRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new NotFoundException("Receipt not found: " + receiptNumber));

        // Verify ownership
        if (!receipt.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access denied to receipt: " + receiptNumber);
        }

        ReceiptResponseDto responseDto = toResponseDto(receipt);

        UnravelDocsResponse<ReceiptResponseDto> response = responseBuilderService.buildUserResponse(
                responseDto,
                HttpStatus.OK,
                "Receipt retrieved successfully"
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Download receipt", description = "Redirect to the receipt PDF download URL")
    @GetMapping("/{receiptNumber}/download")
    public ResponseEntity<UnravelDocsResponse<String>> downloadReceipt(
            @AuthenticationPrincipal User user,
            @PathVariable String receiptNumber) {

        Receipt receipt = receiptRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new NotFoundException("Receipt not found: " + receiptNumber));

        // Verify ownership
        if (!receipt.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access denied to receipt: " + receiptNumber);
        }

        if (receipt.getReceiptUrl() == null || receipt.getReceiptUrl().isEmpty()) {
            throw new NotFoundException("Receipt PDF not available: " + receiptNumber);
        }

        UnravelDocsResponse<String> response = responseBuilderService.buildUserResponse(
                receipt.getReceiptUrl(),
                HttpStatus.OK,
                "Receipt download URL retrieved"
        );

        return ResponseEntity.ok(response);
    }

    private ReceiptResponseDto toResponseDto(Receipt receipt) {
        return ReceiptResponseDto.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .paymentProvider(receipt.getPaymentProvider())
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .paymentMethod(receipt.getPaymentMethod())
                .paymentMethodDetails(receipt.getPaymentMethodDetails())
                .description(receipt.getDescription())
                .receiptUrl(receipt.getReceiptUrl())
                .paidAt(receipt.getPaidAt())
                .createdAt(receipt.getCreatedAt())
                .build();
    }
}
