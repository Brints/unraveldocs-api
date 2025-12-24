package com.extractor.unraveldocs.elasticsearch.service;

import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.elasticsearch.document.DocumentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.document.PaymentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.document.UserSearchIndex;
import com.extractor.unraveldocs.elasticsearch.repository.DocumentSearchRepository;
import com.extractor.unraveldocs.elasticsearch.repository.PaymentSearchRepository;
import com.extractor.unraveldocs.elasticsearch.repository.UserSearchRepository;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for bulk synchronization of data from PostgreSQL to Elasticsearch.
 * Used for initial data migration and re-indexing operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class ElasticsearchSyncService {

    private final UserRepository userRepository;
    private final DocumentCollectionRepository documentCollectionRepository;
    private final OcrDataRepository ocrDataRepository;
    private final ReceiptRepository receiptRepository;

    private final DocumentSearchRepository documentSearchRepository;
    private final UserSearchRepository userSearchRepository;
    private final PaymentSearchRepository paymentSearchRepository;

    private static final int BATCH_SIZE = 100;

    private final SanitizeLogging sanitizer;

    /**
     * Synchronizes all data to Elasticsearch.
     *
     * @return Summary of the sync operation
     */
    @Async
    public CompletableFuture<Map<String, Object>> syncAll() {
        log.info("Starting full Elasticsearch synchronization...");

        Map<String, Object> summary = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            int usersIndexed = syncAllUsers();
            int documentsIndexed = syncAllDocuments();
            int paymentsIndexed = syncAllPayments();

            summary.put("usersIndexed", usersIndexed);
            summary.put("documentsIndexed", documentsIndexed);
            summary.put("paymentsIndexed", paymentsIndexed);
            summary.put("status", "SUCCESS");
            summary.put("durationMs", System.currentTimeMillis() - startTime);

            log.info("Full Elasticsearch sync completed: {} users, {} documents, {} payments in {}ms",
                    sanitizer.sanitizeLoggingInteger(usersIndexed), sanitizer.sanitizeLoggingInteger(documentsIndexed),
                    sanitizer.sanitizeLoggingInteger(paymentsIndexed),
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Full Elasticsearch sync failed: {}", e.getMessage(), e);
            summary.put("status", "FAILED");
            summary.put("error", e.getMessage());
            summary.put("durationMs", System.currentTimeMillis() - startTime);
        }

        return CompletableFuture.completedFuture(summary);
    }

    /**
     * Synchronizes all users to Elasticsearch.
     *
     * @return Number of users indexed
     */
    @Transactional(readOnly = true)
    public int syncAllUsers() {
        log.info("Starting user synchronization to Elasticsearch...");
        int totalIndexed = 0;
        int pageNumber = 0;

        Page<User> page;
        do {
            page = userRepository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));
            List<UserSearchIndex> batch = new ArrayList<>();

            for (User user : page.getContent()) {
                batch.add(mapToUserSearchIndex(user));
            }

            if (!batch.isEmpty()) {
                userSearchRepository.saveAll(batch);
                totalIndexed += batch.size();
                log.debug("Indexed user batch {}: {} users", sanitizer.sanitizeLoggingInteger(pageNumber),
                        sanitizer.sanitizeLoggingInteger(batch.size()));
            }

            pageNumber++;
        } while (page.hasNext());

        log.info("User synchronization completed: {} users indexed", sanitizer.sanitizeLoggingInteger(totalIndexed));
        return totalIndexed;
    }

    /**
     * Synchronizes all documents to Elasticsearch.
     *
     * @return Number of documents indexed
     */
    @Transactional(readOnly = true)
    public int syncAllDocuments() {
        log.info("Starting document synchronization to Elasticsearch...");
        int totalIndexed = 0;
        int pageNumber = 0;

        Page<DocumentCollection> page;
        do {
            page = documentCollectionRepository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));
            List<DocumentSearchIndex> batch = new ArrayList<>();

            for (DocumentCollection collection : page.getContent()) {
                // Collect all document IDs first
                List<String> documentIds = collection.getFiles().stream()
                        .map(FileEntry::getDocumentId)
                        .toList();

                // Batch fetch OCR data
                Map<String, OcrData> ocrDataMap = ocrDataRepository.findByDocumentIdIn(documentIds)
                        .stream()
                        .collect(Collectors.toMap(OcrData::getDocumentId, Function.identity()));

                for (FileEntry file : collection.getFiles()) {
                    OcrData ocrData = ocrDataMap.get(file.getDocumentId());
                    batch.add(mapToDocumentSearchIndex(collection, file, ocrData));
                }
            }

            if (!batch.isEmpty()) {
                documentSearchRepository.saveAll(batch);
                totalIndexed += batch.size();
                log.debug("Indexed document batch {}: {} documents", sanitizer.sanitizeLoggingInteger(pageNumber),
                        sanitizer.sanitizeLoggingInteger(batch.size()));
            }

            pageNumber++;
        } while (page.hasNext());

        log.info("Document synchronization completed: {} documents indexed",
                sanitizer.sanitizeLoggingInteger(totalIndexed));
        return totalIndexed;
    }

    /**
     * Synchronizes all payments/receipts to Elasticsearch.
     *
     * @return Number of payments indexed
     */
    @Transactional(readOnly = true)
    public int syncAllPayments() {
        log.info("Starting payment synchronization to Elasticsearch...");
        int totalIndexed = 0;
        int pageNumber = 0;

        Page<Receipt> page;
        do {
            page = receiptRepository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));
            List<PaymentSearchIndex> batch = new ArrayList<>();

            for (Receipt receipt : page.getContent()) {
                batch.add(mapToPaymentSearchIndex(receipt));
            }

            if (!batch.isEmpty()) {
                paymentSearchRepository.saveAll(batch);
                totalIndexed += batch.size();
                log.debug("Indexed payment batch {}: {} payments", sanitizer.sanitizeLoggingInteger(pageNumber),
                        sanitizer.sanitizeLoggingInteger(batch.size()));
            }

            pageNumber++;
        } while (page.hasNext());

        log.info("Payment synchronization completed: {} payments indexed",
                sanitizer.sanitizeLoggingInteger(totalIndexed));
        return totalIndexed;
    }

    /**
     * Indexes a single user (for real-time updates).
     */
    public void indexUser(User user) {
        UserSearchIndex index = mapToUserSearchIndex(user);
        userSearchRepository.save(index);
        log.debug("Indexed user: {}", sanitizer.sanitizeLogging(user.getId()));
    }

    /**
     * Indexes a single document with OCR data.
     */
    public void indexDocument(DocumentCollection collection, FileEntry file, OcrData ocrData) {
        DocumentSearchIndex index = mapToDocumentSearchIndex(collection, file, ocrData);
        documentSearchRepository.save(index);
        log.debug("Indexed document: {}", sanitizer.sanitizeLogging(file.getDocumentId()));
    }

    /**
     * Indexes a single receipt/payment.
     */
    public void indexPayment(Receipt receipt) {
        PaymentSearchIndex index = mapToPaymentSearchIndex(receipt);
        paymentSearchRepository.save(index);
        log.debug("Indexed payment: {}", sanitizer.sanitizeLogging(receipt.getId()));
    }

    // ==================== Mapping Methods ====================

    private UserSearchIndex mapToUserSearchIndex(User user) {
        return UserSearchIndex.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .isPlatformAdmin(user.isPlatformAdmin())
                .isOrganizationAdmin(user.isOrganizationAdmin())
                .country(user.getCountry())
                .profession(user.getProfession())
                .organization(user.getOrganization())
                .profilePicture(user.getProfilePicture())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .documentCount(user.getDocuments() != null ? user.getDocuments().size() : 0)
                .subscriptionPlan(user.getSubscription() != null && user.getSubscription().getPlan() != null
                        ? user.getSubscription().getPlan().getName().name()
                        : null)
                .subscriptionStatus(user.getSubscription() != null
                        ? user.getSubscription().getStatus()
                        : null)
                .build();
    }

    private DocumentSearchIndex mapToDocumentSearchIndex(DocumentCollection collection, FileEntry file,
            OcrData ocrData) {
        return DocumentSearchIndex.builder()
                .id(file.getDocumentId())
                .userId(collection.getUser().getId())
                .collectionId(collection.getId())
                .fileName(file.getOriginalFileName())
                .fileType(file.getFileType())
                .fileSize(file.getFileSize())
                .status(collection.getCollectionStatus().name())
                .ocrStatus(ocrData != null ? ocrData.getStatus().name() : null)
                .extractedText(ocrData != null ? ocrData.getExtractedText() : null)
                .fileUrl(file.getFileUrl())
                .uploadTimestamp(collection.getUploadTimestamp())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    private PaymentSearchIndex mapToPaymentSearchIndex(Receipt receipt) {
        User user = receipt.getUser();
        return PaymentSearchIndex.builder()
                .id(receipt.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getFirstName() + " " + user.getLastName())
                .receiptNumber(receipt.getReceiptNumber())
                .paymentProvider(receipt.getPaymentProvider().name())
                .externalPaymentId(receipt.getExternalPaymentId())
                .status("COMPLETED") // Receipts are only generated for successful payments
                .amount(receipt.getAmount() != null ? receipt.getAmount().doubleValue() : null)
                .currency(receipt.getCurrency())
                .paymentMethod(receipt.getPaymentMethod())
                .paymentMethodDetails(receipt.getPaymentMethodDetails())
                .description(receipt.getDescription())
                .receiptUrl(receipt.getReceiptUrl())
                .emailSent(receipt.isEmailSent())
                .paidAt(receipt.getPaidAt())
                .createdAt(receipt.getCreatedAt())
                .updatedAt(receipt.getUpdatedAt())
                .build();
    }
}
