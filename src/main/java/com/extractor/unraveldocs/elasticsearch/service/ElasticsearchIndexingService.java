package com.extractor.unraveldocs.elasticsearch.service;

import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.elasticsearch.document.DocumentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.document.PaymentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.document.UserSearchIndex;
import com.extractor.unraveldocs.elasticsearch.events.ElasticsearchIndexEvent;
import com.extractor.unraveldocs.elasticsearch.events.IndexAction;
import com.extractor.unraveldocs.elasticsearch.events.IndexType;
import com.extractor.unraveldocs.elasticsearch.publisher.ElasticsearchEventPublisher;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service for publishing Elasticsearch index events.
 * This service is used by other services to trigger indexing when entities are
 * created or updated.
 * It publishes events to RabbitMQ which are then consumed by
 * ElasticsearchIndexConsumer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class ElasticsearchIndexingService {

    private final ElasticsearchEventPublisher eventPublisher;

    /**
     * Publishes an index event for a user (create or update).
     *
     * @param user   The user to index
     * @param action The action (CREATE or UPDATE)
     */
    public void indexUser(User user, IndexAction action) {
        try {
            UserSearchIndex searchIndex = mapToUserSearchIndex(user);
            String payload = eventPublisher.toJsonPayload(searchIndex);

            ElasticsearchIndexEvent event = action == IndexAction.CREATE
                    ? ElasticsearchIndexEvent.createEvent(user.getId(), IndexType.USER, payload)
                    : ElasticsearchIndexEvent.updateEvent(user.getId(), IndexType.USER, payload);

            eventPublisher.publishUserIndexEvent(event);
            log.debug("Published {} index event for user: {}", action, user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user index event for user {}: {}", user.getId(), e.getMessage());
            // Don't throw - indexing failure should not affect the main operation
        }
    }

    /**
     * Publishes an index event for a document.
     *
     * @param collection The document collection
     * @param file       The file entry
     * @param ocrData    The OCR data (can be null if not yet processed)
     * @param action     The action (CREATE or UPDATE)
     */
    public void indexDocument(DocumentCollection collection, FileEntry file, OcrData ocrData, IndexAction action) {
        try {
            DocumentSearchIndex searchIndex = mapToDocumentSearchIndex(collection, file, ocrData);
            String payload = eventPublisher.toJsonPayload(searchIndex);

            ElasticsearchIndexEvent event = action == IndexAction.CREATE
                    ? ElasticsearchIndexEvent.createEvent(file.getDocumentId(), IndexType.DOCUMENT, payload)
                    : ElasticsearchIndexEvent.updateEvent(file.getDocumentId(), IndexType.DOCUMENT, payload);

            eventPublisher.publishDocumentIndexEvent(event);
            log.debug("Published {} index event for document: {}", action, file.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to publish document index event for document {}: {}",
                    file.getDocumentId(), e.getMessage());
            // Don't throw - indexing failure should not affect the main operation
        }
    }

    /**
     * Publishes an index event for a payment/receipt.
     *
     * @param receipt The receipt to index
     * @param action  The action (CREATE or UPDATE)
     */
    public void indexPayment(Receipt receipt, IndexAction action) {
        try {
            PaymentSearchIndex searchIndex = mapToPaymentSearchIndex(receipt);
            String payload = eventPublisher.toJsonPayload(searchIndex);

            ElasticsearchIndexEvent event = action == IndexAction.CREATE
                    ? ElasticsearchIndexEvent.createEvent(receipt.getId(), IndexType.PAYMENT, payload)
                    : ElasticsearchIndexEvent.updateEvent(receipt.getId(), IndexType.PAYMENT, payload);

            eventPublisher.publishPaymentIndexEvent(event);
            log.debug("Published {} index event for payment: {}", action, receipt.getId());
        } catch (Exception e) {
            log.error("Failed to publish payment index event for receipt {}: {}",
                    receipt.getId(), e.getMessage());
            // Don't throw - indexing failure should not affect the main operation
        }
    }

    /**
     * Publishes a delete event for a user.
     */
    public void deleteUserFromIndex(String userId) {
        try {
            ElasticsearchIndexEvent event = ElasticsearchIndexEvent.deleteEvent(userId, IndexType.USER);
            eventPublisher.publishUserIndexEvent(event);
            log.debug("Published DELETE index event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish user delete index event for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Publishes a delete event for a document.
     */
    public void deleteDocumentFromIndex(String documentId) {
        try {
            ElasticsearchIndexEvent event = ElasticsearchIndexEvent.deleteEvent(documentId, IndexType.DOCUMENT);
            eventPublisher.publishDocumentIndexEvent(event);
            log.debug("Published DELETE index event for document: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to publish document delete index event for document {}: {}", documentId, e.getMessage());
        }
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
