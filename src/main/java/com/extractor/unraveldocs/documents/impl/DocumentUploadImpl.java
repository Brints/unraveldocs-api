package com.extractor.unraveldocs.documents.impl;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionUploadData;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.FileEntryData;
import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import com.extractor.unraveldocs.documents.datamodel.DocumentUploadState;
import com.extractor.unraveldocs.documents.interfaces.DocumentUploadService;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.ocrprocessing.utils.FileStorageService;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.storage.service.StorageAllocationService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.utils.imageupload.FileUploadValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.extractor.unraveldocs.ocrprocessing.utils.FileStorageService.getStorageFailures;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadImpl implements DocumentUploadService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final SanitizeLogging s;
    private final FileStorageService fileStorageService;
    private final StorageAllocationService storageAllocationService;
    private final NotificationService notificationService;
    private final com.extractor.unraveldocs.encryption.interfaces.EncryptionService encryptionService;
    private final com.extractor.unraveldocs.subscription.service.SubscriptionFeatureService subscriptionFeatureService;

    @Override
    @Transactional
    @CacheEvict(value = "documentCollections", key = "#user.id")
    public DocumentCollectionResponse<DocumentCollectionUploadData> uploadDocuments(
            MultipartFile[] files, User user, String collectionName, boolean enableEncryption) {
        // Validate encryption access if enabled
        if (enableEncryption) {
            subscriptionFeatureService.requireFeatureAccess(
                    user.getId(),
                    com.extractor.unraveldocs.subscription.service.SubscriptionFeatureService.Feature.DOCUMENT_ENCRYPTION);
            if (!encryptionService.isEncryptionAvailable()) {
                throw new BadRequestException("Encryption is not available. Please contact support.");
            }
        }

        // Generate collection name if not provided
        String finalCollectionName = (collectionName != null && !collectionName.isBlank())
                ? collectionName.trim()
                : "Collection-" + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        List<FileEntry> processedFileEntries = new ArrayList<>();
        List<FileEntryData> responseFileEntriesData = new ArrayList<>();

        int totalFiles = files.length;
        int successfulUploads = 0;
        int validationFailures = 0;
        int storageFailures = 0;

        FileUploadValidationUtil.validateTotalFileSize(files);

        // Check storage availability before processing uploads
        long totalUploadSize = Arrays.stream(files).mapToLong(MultipartFile::getSize).sum();
        storageAllocationService.checkStorageAvailable(user, totalUploadSize);

        for (MultipartFile file : files) {
            String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed_file");

            FileEntryData.FileEntryDataBuilder fileEntryDataBuilder = FileEntryData.builder()
                    .originalFileName(originalFilename)
                    .fileSize(file.getSize());

            try {
                FileUploadValidationUtil.validateIndividualFile(file);

                try {
                    FileEntry fileEntry = fileStorageService.handleSuccessfulFileUpload(file, originalFilename);

                    processedFileEntries.add(fileEntry);

                    fileEntryDataBuilder.documentId(fileEntry.getDocumentId())
                            .fileUrl(fileEntry.getFileUrl())
                            .status(DocumentUploadState.SUCCESS.toString());
                    successfulUploads++;
                } catch (Exception storageEx) {
                    storageFailures = getStorageFailures(
                            processedFileEntries,
                            storageFailures,
                            originalFilename,
                            storageEx, log, s);
                    fileEntryDataBuilder.status(DocumentUploadState.FAILED_STORAGE_UPLOAD.toString());
                }
            } catch (BadRequestException | IllegalArgumentException validationEx) {
                log.warn("Validation failed for file {}: {}",
                        s.sanitizeLogging(originalFilename),
                        s.sanitizeLogging(validationEx.getMessage()));
                String tempDocumentId = java.util.UUID.randomUUID().toString();
                fileEntryDataBuilder.documentId(tempDocumentId)
                        .status(DocumentUploadState.FAILED_VALIDATION.toString());
                validationFailures++;
            }
            responseFileEntriesData.add(fileEntryDataBuilder.build());
        }

        String savedCollectionId = null;

        if (!processedFileEntries.isEmpty()) {
            DocumentCollection documentCollection = DocumentCollection.builder()
                    .user(user)
                    .name(finalCollectionName)
                    .files(processedFileEntries)
                    .uploadTimestamp(OffsetDateTime.now())
                    .build();

            boolean allProcessedSucceededInStorage = processedFileEntries.stream()
                    .allMatch(fe -> DocumentUploadState.SUCCESS.toString().equals(fe.getUploadStatus()));
            boolean anyProcessedSucceededInStorage = processedFileEntries.stream()
                    .anyMatch(fe -> DocumentUploadState.SUCCESS.toString().equals(fe.getUploadStatus()));

            if (allProcessedSucceededInStorage) {
                documentCollection.setCollectionStatus(DocumentStatus.COMPLETED);
            } else if (anyProcessedSucceededInStorage) {
                documentCollection.setCollectionStatus(DocumentStatus.PARTIALLY_COMPLETED);
            } else {
                documentCollection.setCollectionStatus(DocumentStatus.FAILED_UPLOAD);
            }

            DocumentCollection savedCollection = documentCollectionRepository.save(documentCollection);
            savedCollectionId = savedCollection.getId();

            // Update storage used for successfully uploaded files
            long successfulUploadSize = processedFileEntries.stream()
                    .filter(fe -> DocumentUploadState.SUCCESS.toString().equals(fe.getUploadStatus()))
                    .mapToLong(FileEntry::getFileSize)
                    .sum();
            if (successfulUploadSize > 0) {
                storageAllocationService.updateStorageUsed(user, successfulUploadSize);
            }

            log.info("Document collection {} created with {} processed files for user {}. Status: {}",
                    s.sanitizeLogging(savedCollectionId),
                    processedFileEntries.size(),
                    s.sanitizeLogging(user.getId()),
                    savedCollection.getCollectionStatus());

            // Send push notification for document upload
            sendDocumentUploadNotification(user, savedCollection, successfulUploads, validationFailures,
                    storageFailures);
        } else {
            if (totalFiles > 0) {
                log.info("No document collection created as all {} files failed validation for user {}",
                        totalFiles,
                        s.sanitizeLogging(user.getId()));

                // Send failure notification
                sendDocumentUploadFailureNotification(user, totalFiles);
            }
        }

        DocumentStatus apiResponseOverallStatus;
        if (totalFiles > 0 && successfulUploads == totalFiles) {
            apiResponseOverallStatus = DocumentStatus.COMPLETED;
        } else if (successfulUploads > 0 || (storageFailures > 0 && !processedFileEntries.isEmpty())) {
            apiResponseOverallStatus = DocumentStatus.PARTIALLY_COMPLETED;
        } else {
            apiResponseOverallStatus = DocumentStatus.FAILED_UPLOAD;
        }

        String apiResponseStatusString;
        String apiResponseMessage;
        if (totalFiles > 0 && successfulUploads == totalFiles) {
            apiResponseStatusString = "success";
            apiResponseMessage = "All " + totalFiles + " document(s) uploaded successfully.";
        } else if (successfulUploads > 0) {
            apiResponseStatusString = "partial_success";
            apiResponseMessage = String.format(
                    "%d document(s) uploaded successfully. %d failed validation. %d failed storage. Check individual statuses.",
                    successfulUploads, validationFailures, storageFailures);
        } else {
            apiResponseStatusString = "failure";
            if (totalFiles > 0 && validationFailures == totalFiles) {
                apiResponseMessage = "All " + totalFiles
                        + " document(s) failed validation. No documents were uploaded.";
            } else if (totalFiles > 0) {
                apiResponseMessage = String.format(
                        "No documents were successfully uploaded. %d failed validation. %d failed storage. Check individual statuses.",
                        validationFailures, storageFailures);
            } else {
                apiResponseMessage = "No files provided for upload.";
            }
        }

        DocumentCollectionUploadData uploadData = DocumentCollectionUploadData.builder()
                .collectionId(savedCollectionId)
                .overallStatus(apiResponseOverallStatus)
                .files(responseFileEntriesData)
                .build();

        return DocumentCollectionResponse.<DocumentCollectionUploadData>builder()
                .statusCode(HttpStatus.OK.value())
                .status(apiResponseStatusString)
                .message(apiResponseMessage)
                .data(uploadData)
                .build();
    }

    /**
     * Send push notification for successful/partial document upload.
     */
    private void sendDocumentUploadNotification(User user, DocumentCollection collection,
            int successfulUploads, int validationFailures, int storageFailures) {
        try {
            NotificationType type;
            String title;
            String message;

            if (collection.getCollectionStatus() == DocumentStatus.COMPLETED) {
                type = NotificationType.DOCUMENT_UPLOAD_SUCCESS;
                title = "Documents Uploaded Successfully";
                message = String.format("%d document(s) uploaded successfully. You can see your uploaded document in the Documents section and run an OCR extraction on it. Thank you for using UnravelDocs", successfulUploads);
            } else if (collection.getCollectionStatus() == DocumentStatus.PARTIALLY_COMPLETED) {
                type = NotificationType.DOCUMENT_UPLOAD_SUCCESS;
                title = "Documents Partially Uploaded";
                message = String.format("%d document(s) uploaded. %d failed. Please check individual document statuses in the Documents section.", successfulUploads, (validationFailures + storageFailures));
            } else {
                type = NotificationType.DOCUMENT_UPLOAD_FAILED;
                title = "Document Upload Failed";
                message = "All documents failed to upload. Please try again. You can contact support if the issue persists.";
            }

            Map<String, String> data = new HashMap<>();
            data.put("collectionId", collection.getId());
            data.put("status", collection.getCollectionStatus().toString());
            data.put("successCount", String.valueOf(successfulUploads));

            notificationService.sendToUser(user.getId(), type, title, message, data);
            log.debug("Sent document upload notification to user {}", s.sanitizeLogging(user.getId()));
        } catch (Exception e) {
            log.error("Failed to send document upload notification: {}", e.getMessage());
        }
    }

    /**
     * Send push notification when all documents fail validation.
     */
    private void sendDocumentUploadFailureNotification(User user, int totalFiles) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("failedCount", String.valueOf(totalFiles));

            notificationService.sendToUser(
                    user.getId(),
                    NotificationType.DOCUMENT_UPLOAD_FAILED,
                    "Document Upload Failed",
                    String.format("All %d document(s) failed validation.", totalFiles),
                    data);
            log.debug("Sent document upload failure notification to user {}", s.sanitizeLogging(user.getId()));
        } catch (Exception e) {
            log.error("Failed to send document upload failure notification: {}", e.getMessage());
        }
    }
}