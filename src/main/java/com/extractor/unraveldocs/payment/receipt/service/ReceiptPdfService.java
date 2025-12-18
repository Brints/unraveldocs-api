package com.extractor.unraveldocs.payment.receipt.service;

import com.extractor.unraveldocs.payment.receipt.config.ReceiptConfig;
import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import static com.extractor.unraveldocs.payment.receipt.service.ReceiptEmailService.getString;

/**
 * Service for generating PDF receipts using OpenPDF
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptPdfService {

    private final ReceiptConfig receiptConfig;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm");
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(37, 99, 235));
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(55, 65, 81));
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(17, 24, 39));
    private static final Font AMOUNT_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(22, 163, 74));
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(107, 114, 128));

    /**
     * Generate a PDF receipt
     *
     * @param receiptNumber Unique receipt number
     * @param data Receipt data
     * @return PDF content as byte array
     */
    public byte[] generateReceiptPdf(String receiptNumber, ReceiptData data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Add header
            addHeader(document, receiptNumber);

            // Add payment confirmation badge
            addPaymentConfirmation(document);

            // Add receipt details table
            addReceiptDetails(document, receiptNumber, data);

            // Add amount section
            addAmountSection(document, data);

            // Add footer
            addFooter(document);

            document.close();

            log.info("Generated PDF receipt: {}", receiptNumber);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF receipt: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF receipt", e);
        }
    }

    private void addHeader(Document document, String receiptNumber) throws DocumentException {
        // Company name
        Paragraph companyName = new Paragraph(receiptConfig.getCompanyName(), TITLE_FONT);
        companyName.setAlignment(Element.ALIGN_CENTER);
        document.add(companyName);

        // Receipt title
        Paragraph receiptTitle = new Paragraph("PAYMENT RECEIPT",
                new Font(Font.HELVETICA, 14, Font.NORMAL, new Color(107, 114, 128)));
        receiptTitle.setAlignment(Element.ALIGN_CENTER);
        receiptTitle.setSpacingBefore(5);
        document.add(receiptTitle);

        // Receipt number
        Paragraph receiptNum = new Paragraph("Receipt #: " + receiptNumber,
                new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(107, 114, 128)));
        receiptNum.setAlignment(Element.ALIGN_CENTER);
        receiptNum.setSpacingBefore(5);
        receiptNum.setSpacingAfter(20);
        document.add(receiptNum);
    }

    private void addPaymentConfirmation(Document document) throws DocumentException {
        PdfPTable confirmTable = new PdfPTable(1);
        confirmTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(220, 252, 231)); // Light green
        cell.setBorderColor(new Color(34, 197, 94)); // Green border
        cell.setPadding(15);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph confirmText = new Paragraph("✓ Payment Successful",
                new Font(Font.HELVETICA, 14, Font.BOLD, new Color(22, 163, 74)));
        confirmText.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(confirmText);

        confirmTable.addCell(cell);
        confirmTable.setSpacingAfter(25);
        document.add(confirmTable);
    }

    private void addReceiptDetails(Document document, String receiptNumber, ReceiptData data) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        // Add details with alternating colors
        addDetailRow(table, "Receipt Number", receiptNumber, true);
        addDetailRow(table, "Date", data.getPaidAt() != null ?
                data.getPaidAt().format(DATE_FORMATTER) : "N/A", false);
        addDetailRow(table, "Customer", data.getCustomerName(), true);
        addDetailRow(table, "Email", data.getCustomerEmail(), false);
        addDetailRow(table, "Payment Method", formatPaymentMethod(data), true);
        addDetailRow(table, "Description", data.getDescription() != null ?
                data.getDescription() : "Subscription Payment", false);
        addDetailRow(table, "Payment Provider", data.getPaymentProvider().name(), true);

        table.setSpacingAfter(25);
        document.add(table);
    }

    private void addDetailRow(PdfPTable table, String label, String value, boolean alternate) {
        Color bgColor = alternate ? new Color(249, 250, 251) : Color.WHITE;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(bgColor);
        labelCell.setPadding(10);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", VALUE_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setBackgroundColor(bgColor);
        valueCell.setPadding(10);
        table.addCell(valueCell);
    }

    private void addAmountSection(Document document, ReceiptData data) throws DocumentException {
        PdfPTable amountTable = new PdfPTable(1);
        amountTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(239, 246, 255)); // Light blue
        cell.setBorderColor(new Color(37, 99, 235)); // Blue border
        cell.setPadding(20);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph amountLabel = new Paragraph("Amount Paid",
                new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(107, 114, 128)));
        amountLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(amountLabel);

        String formattedAmount = formatCurrency(data.getAmount().doubleValue(), data.getCurrency());
        Paragraph amountValue = new Paragraph(formattedAmount, AMOUNT_FONT);
        amountValue.setAlignment(Element.ALIGN_CENTER);
        amountValue.setSpacingBefore(5);
        cell.addElement(amountValue);

        amountTable.addCell(cell);
        amountTable.setSpacingAfter(30);
        document.add(amountTable);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);

        footer.add(new Chunk("Thank you for your payment!\n", FOOTER_FONT));

        if (receiptConfig.getCompanyEmail() != null) {
            footer.add(new Chunk("Questions? Contact us at " + receiptConfig.getCompanyEmail() + "\n", FOOTER_FONT));
        }

        footer.add(new Chunk("\n© " + java.time.Year.now().getValue() + " " +
                receiptConfig.getCompanyName() + ". All rights reserved.", FOOTER_FONT));

        document.add(footer);
    }

    private String formatPaymentMethod(ReceiptData data) {
        if (data.getPaymentMethodDetails() != null && !data.getPaymentMethodDetails().isEmpty()) {
            return data.getPaymentMethodDetails();
        }
        return data.getPaymentMethod() != null ? data.getPaymentMethod() : "Card";
    }

    private String formatCurrency(double amount, String currency) {
        return getString(amount, currency);
    }
}
