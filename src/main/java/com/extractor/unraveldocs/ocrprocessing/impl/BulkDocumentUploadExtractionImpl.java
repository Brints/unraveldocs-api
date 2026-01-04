package com.extractor.unraveldocs.ocrprocessing.impl;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionUploadData;
import com.extractor.unraveldocs.documents.dto.response.FileEntryData;
import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import com.extractor.unraveldocs.documents.datamodel.DocumentUploadState;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.events.OcrEventMapper;
import com.extractor.unraveldocs.ocrprocessing.events.OcrEventPublisher;
import com.extractor.unraveldocs.ocrprocessing.events.OcrRequestedEvent;
import com.extractor.unraveldocs.ocrprocessing.interfaces.BulkDocumentUploadExtractionService;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.ocrprocessing.utils.FileStorageService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.utils.imageupload.FileUploadValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.extractor.unraveldocs.ocrprocessing.utils.FileStorageService.getStorageFailures;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkDocumentUploadExtractionImpl implements BulkDocumentUploadExtractionService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final OcrDataRepository ocrDataRepository;
    private final Optional<OcrEventPublisher> ocrEventPublisher;
    private final OcrEventMapper ocrEventMapper;
    private final SanitizeLogging s;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public DocumentCollectionResponse<DocumentCollectionUploadData> uploadDocuments(MultipartFile[] files, User user) {
        List<FileEntry> processedFiles = new ArrayList<>();
        List<FileEntryData> responseFileEntriesData = new ArrayList<>();
        List<OcrData> ocrDataToSave = new ArrayList<>();

        int totalFiles = files.length;
        int successfulUploads = 0;
        int validationFailures = 0;
        int storageFailures = 0;

        FileUploadValidationUtil.validateTotalFileSize(files);

        for (MultipartFile file : files) {
            String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed_file");

            FileEntryData.FileEntryDataBuilder fileEntryDataBuilder = FileEntryData.builder()
                    .originalFileName(originalFilename)
                    .fileSize(file.getSize());

            try {
                FileUploadValidationUtil.validateIndividualFile(file);

                try {
                    FileEntry fileEntry = fileStorageService.handleSuccessfulFileUpload(file, originalFilename);
                    processedFiles.add(fileEntry);

                    OcrData ocrData = new OcrData();
                    ocrData.setDocumentId(fileEntry.getDocumentId());
                    ocrData.setStatus(OcrStatus.PENDING);
                    ocrDataToSave.add(ocrData);

                    fileEntryDataBuilder.documentId(fileEntry.getDocumentId())
                            .fileUrl(fileEntry.getFileUrl())
                            .status(DocumentUploadState.SUCCESS.toString());
                    successfulUploads++;
                } catch (Exception e) {
                    storageFailures = getStorageFailures(processedFiles, storageFailures, originalFilename, e, log, s);
                    log.warn("File {} failed to upload to storage: {}", s.sanitizeLogging(originalFilename),
                            s.sanitizeLogging(e.getMessage()));
                    fileEntryDataBuilder.status(DocumentUploadState.FAILED_STORAGE_UPLOAD.toString());
                }
            } catch (BadRequestException | IllegalArgumentException validationEx) {
                log.warn("Validation failed for file {}: {}", s.sanitizeLogging(originalFilename),
                        s.sanitizeLogging(validationEx.getMessage()));
                String tempDocumentId = java.util.UUID.randomUUID().toString();
                fileEntryDataBuilder.documentId(tempDocumentId)
                        .status(DocumentUploadState.FAILED_VALIDATION.toString());
                validationFailures++;
            }
            responseFileEntriesData.add(fileEntryDataBuilder.build());
        }

        String savedCollectionId = null;

        if (!processedFiles.isEmpty()) {
            DocumentCollection documentCollection = DocumentCollection.builder()
                    .user(user)
                    .files(processedFiles)
                    .uploadTimestamp(OffsetDateTime.now())
                    .build();

            boolean anyProcessedSucceededInStorage = processedFiles.stream()
                    .anyMatch(fe -> DocumentUploadState.SUCCESS.toString().equals(fe.getUploadStatus()));

            if (anyProcessedSucceededInStorage) {
                documentCollection.setCollectionStatus(DocumentStatus.PROCESSING);
            } else {
                documentCollection.setCollectionStatus(DocumentStatus.FAILED_UPLOAD);
            }

            // Flush the insert of DocumentCollection and cascaded FileEntry rows before
            // saving OCR data
            DocumentCollection savedCollection = documentCollectionRepository.saveAndFlush(documentCollection);
            savedCollectionId = savedCollection.getId();

            if (!ocrDataToSave.isEmpty()) {
                ocrDataRepository.saveAll(ocrDataToSave);
                ocrDataRepository.flush(); // Ensure all OCR data is saved before proceeding
            }

            String finalSavedCollectionId = savedCollectionId;
            List<FileEntry> successfulFiles = savedCollection.getFiles().stream()
                    .filter(fe -> DocumentUploadState.SUCCESS.toString().equals(fe.getUploadStatus()))
                    .toList();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ocrEventPublisher.ifPresent(publisher -> {
                        successfulFiles.forEach(fileEntry -> {
                            OcrRequestedEvent event = ocrEventMapper.toOcrRequestedEvent(fileEntry,
                                    finalSavedCollectionId);
                            publisher.publishOcrRequest(event);
                        });
                    });
                }
            });

            log.info("Document collection {} created with {} processed files for user {}. Status: {}",
                    s.sanitizeLogging(savedCollectionId), processedFiles.size(), s.sanitizeLogging(user.getId()),
                    savedCollection.getCollectionStatus());
        } else {
            if (totalFiles > 0) {
                log.info("No document collection created as all {} files failed validation for user {}", totalFiles,
                        s.sanitizeLogging(user.getId()));
            }
        }

        String apiResponseMessage;
        if (successfulUploads > 0) {
            apiResponseMessage = String.format(
                    "%d document(s) uploaded successfully and queued for processing. %d failed.", successfulUploads,
                    validationFailures + storageFailures);
        } else {
            apiResponseMessage = "All document uploads failed.";
        }

        DocumentCollectionUploadData uploadData = DocumentCollectionUploadData.builder()
                .collectionId(savedCollectionId)
                .overallStatus(successfulUploads > 0 ? DocumentStatus.PROCESSING : DocumentStatus.FAILED_UPLOAD)
                .files(responseFileEntriesData)
                .build();

        return DocumentCollectionResponse.<DocumentCollectionUploadData>builder()
                .statusCode(HttpStatus.ACCEPTED.value())
                .status(OcrStatus.PROCESSING.getStatus())
                .message(apiResponseMessage)
                .data(uploadData)
                .build();
    }
}