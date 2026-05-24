package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.interfaces.AdminReceiptActionsService;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.payment.receipt.service.ReceiptEmailService;
import com.extractor.unraveldocs.payment.receipt.service.ReceiptPdfService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReceiptActionsServiceImpl implements AdminReceiptActionsService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptPdfService receiptPdfService;
    private final ReceiptEmailService receiptEmailService;
    private final ResponseBuilderService responseBuilder;

    @Override
    @Transactional
    public UnravelDocsResponse<String> resendReceiptEmail(String receiptId) {
        log.info("Admin resending receipt email for receipt ID: {}", receiptId);

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));

        User user = receipt.getUser();
        if (user == null || user.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt does not have a valid associated user email");
        }

        try {
            // Reconstruct ReceiptData
            ReceiptData data = ReceiptData.builder()
                    .userId(user.getId())
                    .customerName(user.getFirstName() + " " + user.getLastName())
                    .customerEmail(user.getEmail())
                    .paymentProvider(receipt.getPaymentProvider())
                    .externalPaymentId(receipt.getExternalPaymentId())
                    .amount(receipt.getAmount())
                    .currency(receipt.getCurrency())
                    .paymentMethod(receipt.getPaymentMethod())
                    .paymentMethodDetails(receipt.getPaymentMethodDetails())
                    .description(receipt.getDescription())
                    .paidAt(receipt.getPaidAt())
                    .build();

            // Generate PDF bytes
            byte[] pdfContent = receiptPdfService.generateReceiptPdf(receipt.getReceiptNumber(), data);

            // Send async email
            receiptEmailService.sendReceiptEmail(receipt.getReceiptNumber(), data, pdfContent);

            // Update status (synchronously here, although mail is async)
            receipt.setEmailSent(true);
            receipt.setEmailSentAt(OffsetDateTime.now());
            receiptRepository.save(receipt);

            log.info("Successfully re-triggered receipt email for receipt ID: {}", receiptId);

            return responseBuilder.buildUserResponse("Receipt email resend triggered successfully", HttpStatus.OK, "Success");
        } catch (Exception e) {
            log.error("Failed to resend receipt email for receipt ID {}: {}", receiptId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resend receipt email: " + e.getMessage());
        }
    }
}
