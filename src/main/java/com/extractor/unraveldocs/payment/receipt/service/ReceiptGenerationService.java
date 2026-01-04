package com.extractor.unraveldocs.payment.receipt.service;

import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.payment.receipt.config.ReceiptConfig;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.elasticsearch.events.IndexAction;
import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchIndexingService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main service for receipt generation orchestration.
 * This service is invoked by the ReceiptMessageListener after receiving
 * a receipt generation request from Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptGenerationService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final ReceiptPdfService receiptPdfService;
    private final ReceiptStorageService receiptStorageService;
    private final ReceiptEmailService receiptEmailService;
    private final ReceiptConfig receiptConfig;
    private final Optional<ElasticsearchIndexingService> elasticsearchIndexingService;

    private static final AtomicLong receiptCounter = new AtomicLong(System.currentTimeMillis());

    /**
     * Process receipt generation for a payment.
     * This method is called by the Kafka listener (ReceiptMessageListener)
     * and runs synchronously within the consumer thread.
     *
     * @param data Receipt data from payment
     * @return Generated Receipt entity, or null if already exists (idempotency)
     */
    @Transactional
    public Receipt processReceiptGeneration(ReceiptData data) {
        log.info("Processing receipt generation for payment: {} via {}",
                data.getExternalPaymentId(), data.getPaymentProvider());

        // Idempotency check - prevent duplicate receipts
        if (receiptExists(data.getExternalPaymentId(), data.getPaymentProvider())) {
            log.info("Receipt already exists for payment: {}", data.getExternalPaymentId());
            return receiptRepository.findByExternalPaymentIdAndPaymentProvider(
                    data.getExternalPaymentId(), data.getPaymentProvider()).orElse(null);
        }

        try {
            // Get user
            User user = userRepository.findById(data.getUserId())
                    .orElseThrow(() -> new NotFoundException("User not found: " + data.getUserId()));

            // Generate unique receipt number
            String receiptNumber = generateReceiptNumber();

            // Generate PDF
            byte[] pdfContent = receiptPdfService.generateReceiptPdf(receiptNumber, data);

            // Upload to S3
            String receiptUrl = receiptStorageService.uploadReceipt(pdfContent, receiptNumber);

            // Save receipt entity
            Receipt receipt = Receipt.builder()
                    .user(user)
                    .receiptNumber(receiptNumber)
                    .paymentProvider(data.getPaymentProvider())
                    .externalPaymentId(data.getExternalPaymentId())
                    .amount(data.getAmount())
                    .currency(data.getCurrency())
                    .paymentMethod(data.getPaymentMethod())
                    .paymentMethodDetails(data.getPaymentMethodDetails())
                    .description(data.getDescription())
                    .receiptUrl(receiptUrl)
                    .paidAt(data.getPaidAt())
                    .emailSent(false)
                    .build();

            final Receipt savedReceipt = receiptRepository.save(receipt);
            log.info("Receipt saved: {}", receiptNumber);

            // Index payment in Elasticsearch
            elasticsearchIndexingService.ifPresent(service -> service.indexPayment(savedReceipt, IndexAction.CREATE));

            // Send email (async)
            sendReceiptEmailAsync(savedReceipt, data, pdfContent);

            return savedReceipt;

        } catch (Exception e) {
            log.error("Failed to generate receipt for payment {}: {}",
                    data.getExternalPaymentId(), e.getMessage());
            throw new RuntimeException("Failed to generate receipt", e);
        }
    }

    /**
     * Check if receipt already exists for a payment (idempotency)
     */
    public boolean receiptExists(String externalPaymentId, PaymentProvider provider) {
        return receiptRepository.existsByExternalPaymentIdAndPaymentProvider(externalPaymentId, provider);
    }

    /**
     * Get receipt by receipt number
     */
    public Optional<Receipt> getReceiptByNumber(String receiptNumber) {
        return receiptRepository.findByReceiptNumber(receiptNumber);
    }

    /**
     * Generate a unique receipt number
     * Format: RCP-YYYYMMDD-XXXXXX
     */
    private String generateReceiptNumber() {
        String datePrefix = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = receiptCounter.incrementAndGet() % 1000000;
        return String.format("%s-%s-%06d",
                receiptConfig.getReceiptPrefix(),
                datePrefix,
                sequence);
    }

    private void sendReceiptEmailAsync(Receipt receipt, ReceiptData data, byte[] pdfContent) {
        try {
            receiptEmailService.sendReceiptEmail(receipt.getReceiptNumber(), data, pdfContent);

            // Update email sent status
            receipt.setEmailSent(true);
            receipt.setEmailSentAt(OffsetDateTime.now());
            receiptRepository.save(receipt);

            log.info("Receipt email sent and status updated for: {}", receipt.getReceiptNumber());
        } catch (Exception e) {
            log.error("Failed to send receipt email for {}: {}", receipt.getReceiptNumber(), e.getMessage());
            // Don't throw - receipt is already saved, email failure shouldn't rollback
        }
    }
}
