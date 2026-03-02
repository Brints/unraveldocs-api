package com.extractor.unraveldocs.ocrprocessing.impl;

import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.interfaces.ExtractTextFromDocumentService;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrRequest;
import com.extractor.unraveldocs.ocrprocessing.provider.OcrResult;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.ocrprocessing.service.OcrProcessingService;
import com.extractor.unraveldocs.ocrprocessing.utils.FindAndValidateFileEntry;
import com.extractor.unraveldocs.credit.service.CreditBalanceService;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractTextFromDocumentImpl implements ExtractTextFromDocumentService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final OcrDataRepository ocrDataRepository;
    private final FindAndValidateFileEntry validateFileEntry;
    private final SanitizeLogging sanitizer;
    private final OcrProcessingService ocrProcessingService;
    private final CreditBalanceService creditBalanceService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OcrData extractTextFromDocument(String collectionId, String documentId, String userId) {
        return extractTextFromDocument(collectionId, documentId, userId, null, null, null);
    }

    @Override
    @Transactional
    public OcrData extractTextFromDocument(String collectionId, String documentId, String userId,
            Integer startPage, Integer endPage, List<Integer> pages) {
        FileEntry fileEntry = validateFileEntry
                .findAndValidateFileEntry(collectionId, documentId, userId, documentCollectionRepository);

        Optional<OcrData> existingOcrData = ocrDataRepository.findByDocumentId(documentId);
        if (existingOcrData.isPresent()) {
            log.info("OCR data already exists for document ID: {}", sanitizer.sanitizeLogging(documentId));
            return existingOcrData.get();
        }

        OcrData ocrData = new OcrData();
        ocrData.setDocumentId(documentId);
        try {
            log.info("Starting OCR text extraction for document: {}", sanitizer.sanitizeLogging(documentId));
            ocrData.setStatus(OcrStatus.PROCESSING);
            ocrDataRepository.save(ocrData);

            // Build OCR request and process through provider abstraction
            OcrRequest ocrRequest = OcrRequest.builder()
                    .documentId(documentId)
                    .collectionId(collectionId)
                    .imageUrl(fileEntry.getFileUrl())
                    .mimeType(fileEntry.getFileType())
                    .userId(userId)
                    .startPage(startPage)
                    .endPage(endPage)
                    .pages(pages)
                    .fallbackEnabled(true)
                    .build();

            // Provider selected automatically based on plan + credits
            OcrResult result = ocrProcessingService.processOcr(ocrRequest, userId);

            if (result.isSuccess()) {
                ocrData.setExtractedText(result.getExtractedText());
                ocrData.setStatus(OcrStatus.COMPLETED);
                log.info("OCR text extraction completed for document: {} using provider: {}",
                        sanitizer.sanitizeLogging(documentId), result.getProviderType());

                // Deduct credits only for free plan users when Google Vision was used
                if (ocrProcessingService.shouldDeductCredits(userId, result.getProviderType())) {
                    try {
                        userRepository.findById(userId).ifPresent(user -> {
                            if (creditBalanceService.hasEnoughCredits(userId, 1)) {
                                creditBalanceService.deductCredits(user, 1, documentId,
                                        "OCR processing (Google Vision) for document " + documentId);
                                log.info("Deducted 1 credit for Google Vision OCR of document {} for user {}",
                                        sanitizer.sanitizeLogging(documentId),
                                        sanitizer.sanitizeLogging(userId));
                            }
                        });
                    } catch (Exception e) {
                        log.error("Failed to deduct credit for user {}: {}",
                                sanitizer.sanitizeLogging(userId), e.getMessage(), e);
                    }
                }
            } else {
                ocrData.setStatus(OcrStatus.FAILED);
                ocrData.setErrorMessage(result.getErrorMessage());
                log.error("OCR processing failed for document {}: {}",
                        sanitizer.sanitizeLogging(documentId), result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("An unexpected error occurred while processing document {}: {}",
                    sanitizer.sanitizeLogging(documentId), e.getMessage(), e);
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setErrorMessage("An unexpected error occurred: " + e.getMessage());
            throw new ServiceException(
                    "Failed to process OCR for document: " + sanitizer.sanitizeLogging(documentId), e);
        } catch (Throwable t) {
            log.error("Fatal error during OCR processing for document {} (possible native library issue): {}",
                    sanitizer.sanitizeLogging(documentId), t.getMessage(), t);
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setErrorMessage("Fatal OCR error: " + t.getMessage());
            ocrDataRepository.save(ocrData);
            throw new ServiceException(
                    "Fatal error processing OCR for document: " + sanitizer.sanitizeLogging(documentId),
                    new RuntimeException(t));
        }

        return ocrDataRepository.save(ocrData);
    }
}
