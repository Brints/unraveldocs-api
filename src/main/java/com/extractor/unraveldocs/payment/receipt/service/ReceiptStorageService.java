package com.extractor.unraveldocs.payment.receipt.service;

import com.extractor.unraveldocs.utils.imageupload.FileFolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Service for storing receipts in AWS S3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private static final String RECEIPT_FOLDER = FileFolder.RECEIPT.getFolder();

    /**
     * Upload receipt PDF to S3
     *
     * @param pdfContent    PDF content as byte array
     * @param receiptNumber Unique receipt number for filename
     * @return S3 URL of the uploaded receipt
     */
    public String uploadReceipt(byte[] pdfContent, String receiptNumber) {
        String fileName = RECEIPT_FOLDER + receiptNumber + ".pdf";

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("application/pdf")
                    .contentDisposition("attachment; filename=\"" + receiptNumber + ".pdf\"")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(pdfContent));

            // Use S3 direct URL instead of CloudFront
            String receiptUrl = s3Client.utilities()
                    .getUrl(builder -> builder.bucket(bucketName).key(fileName))
                    .toString();
            log.info("Receipt uploaded successfully: {}", receiptUrl);
            return receiptUrl;

        } catch (S3Exception e) {
            log.error("Failed to upload receipt to S3: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to upload receipt to S3", e);
        }
    }

    /**
     * Delete receipt from S3
     *
     * @param receiptUrl S3 URL of the receipt
     */
    public void deleteReceipt(String receiptUrl) {
        if (receiptUrl == null || receiptUrl.isEmpty()) {
            return;
        }

        try {
            // Extract key from S3 URL
            String key = extractKeyFromUrl(receiptUrl);

            software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest = software.amazon.awssdk.services.s3.model.DeleteObjectRequest
                    .builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Receipt deleted successfully: {}", key);

        } catch (S3Exception e) {
            log.error("Failed to delete receipt from S3: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to delete receipt from S3", e);
        }
    }

    /**
     * Extract S3 key from URL
     */
    private String extractKeyFromUrl(String url) {
        // Handle both S3 URL formats:
        if (url.contains(bucketName)) {
            int bucketIndex = url.indexOf(bucketName);
            int keyStart = url.indexOf("/", bucketIndex + bucketName.length());
            if (keyStart >= 0) {
                return url.substring(keyStart + 1);
            }
        }
        // Fallback: assume key is after the last occurrence of bucket name
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
