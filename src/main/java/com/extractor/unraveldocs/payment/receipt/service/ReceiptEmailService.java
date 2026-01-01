package com.extractor.unraveldocs.payment.receipt.service;

import com.extractor.unraveldocs.messaging.emailservice.mailgun.service.MailgunEmailService;
import com.extractor.unraveldocs.messaging.thymleafservice.ThymeleafEmailService;
import com.extractor.unraveldocs.payment.receipt.config.ReceiptConfig;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending receipt emails
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptEmailService {

    private final MailgunEmailService mailgunEmailService;
    private final ThymeleafEmailService thymeleafEmailService;
    private final ReceiptConfig receiptConfig;

    private static final String RECEIPT_TEMPLATE = "paymentReceipt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    /**
     * Send receipt email with PDF attachment
     *
     * @param receiptNumber Receipt number
     * @param data          Receipt data
     * @param pdfContent    PDF content
     */
    @Async
    public void sendReceiptEmail(String receiptNumber, ReceiptData data, byte[] pdfContent) {
        File tempFile = null;
        try {
            // Create temp file for attachment
            tempFile = createTempPdfFile(receiptNumber, pdfContent);

            // Build template model
            Map<String, Object> templateModel = buildTemplateModel(receiptNumber, data);

            // Generate HTML body
            String htmlBody = thymeleafEmailService.createEmail(RECEIPT_TEMPLATE, templateModel);

            // Send email with attachment
            String subject = "Your Payment Receipt - " + receiptNumber;
            mailgunEmailService.sendHtmlWithAttachment(
                    data.getCustomerEmail(),
                    subject,
                    htmlBody,
                    tempFile);

            log.info("Receipt email sent successfully to {} for receipt {}", data.getCustomerEmail(), receiptNumber);

        } catch (Exception e) {
            log.error("Failed to send receipt email for {}: {}", receiptNumber, e.getMessage());
            throw new RuntimeException("Failed to send receipt email", e);
        } finally {
            // Clean up temp file
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    private File createTempPdfFile(String receiptNumber, byte[] pdfContent) throws IOException {
        // File tempFile = File.createTempFile("receipt_" + receiptNumber + "_",
        // ".pdf");
        File tempFile = Files.createTempFile("receipt_" + receiptNumber + "_", ".pdf").toFile();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(pdfContent);
        }
        return tempFile;
    }

    private Map<String, Object> buildTemplateModel(String receiptNumber, ReceiptData data) {
        Map<String, Object> model = new HashMap<>();
        model.put("receiptNumber", receiptNumber);
        model.put("customerName", data.getCustomerName());
        model.put("amount", formatCurrency(data.getAmount().doubleValue(), data.getCurrency()));
        model.put("currency", data.getCurrency());
        model.put("paymentMethod",
                data.getPaymentMethodDetails() != null ? data.getPaymentMethodDetails() : data.getPaymentMethod());
        model.put("description", data.getDescription() != null ? data.getDescription() : "Subscription Payment");
        model.put("paidAt", data.getPaidAt() != null ? data.getPaidAt().format(DATE_FORMATTER) : "N/A");
        model.put("appName", receiptConfig.getCompanyName());
        model.put("supportEmail", receiptConfig.getCompanyEmail());
        return model;
    }

    private String formatCurrency(double amount, String currency) {
        return getString(amount, currency);
    }

    @NonNull
    static String getString(double amount, String currency) {
        String symbol = switch (currency.toUpperCase()) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "NGN" -> "₦";
            default -> currency + " ";
        };
        return symbol + String.format("%,.2f", amount);
    }
}
