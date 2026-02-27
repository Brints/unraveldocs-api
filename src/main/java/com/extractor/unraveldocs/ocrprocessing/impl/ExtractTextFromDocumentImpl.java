package com.extractor.unraveldocs.ocrprocessing.impl;

import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.dto.request.PdfPageRange;
import com.extractor.unraveldocs.ocrprocessing.interfaces.ExtractTextFromDocumentService;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.ocrprocessing.utils.FindAndValidateFileEntry;
import com.extractor.unraveldocs.credit.service.CreditBalanceService;
import com.extractor.unraveldocs.subscription.service.SubscriptionFeatureService;
import com.extractor.unraveldocs.storage.service.StorageAllocationService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.extractor.unraveldocs.ocrprocessing.utils.ExtractImageURL.extractImageURL;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractTextFromDocumentImpl implements ExtractTextFromDocumentService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final OcrDataRepository ocrDataRepository;
    private final FindAndValidateFileEntry validateFileEntry;
    private final SanitizeLogging sanitizer;
    private final StorageAllocationService storageAllocationService;
    private final CreditBalanceService creditBalanceService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final UserRepository userRepository;

    @Value("${tesseract.datapath}")
    private String tesseractDataPath;

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

            extractImageURL(fileEntry, ocrData, tesseractDataPath,
                    buildPageRange(startPage, endPage, pages));
            log.info("OCR text extraction completed for document: {}", sanitizer.sanitizeLogging(documentId));

            try {
                // Update OCR pages used (for monthly quota tracking)
                storageAllocationService.updateOcrUsage(userId, 1);
            } catch (Exception e) {
                log.error("Failed to update OCR usage for user {}: {}", sanitizer.sanitizeLogging(userId),
                        e.getMessage(), e);
            }

            // Deduct credits for users without an active paid subscription
            try {
                if (!subscriptionFeatureService.hasPaidSubscription(userId)) {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null && creditBalanceService.hasEnoughCredits(userId, 1)) {
                        creditBalanceService.deductCredits(user, 1, documentId,
                                "OCR processing: 1 page for document " + documentId);
                        log.info("Deducted 1 credit for OCR processing of document {} for user {}",
                                sanitizer.sanitizeLogging(documentId), sanitizer.sanitizeLogging(userId));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to deduct credit for user {}: {}", sanitizer.sanitizeLogging(userId),
                        e.getMessage(), e);
            }
        } catch (IOException | TesseractException e) {
            log.error("OCR processing failed for document {}: {}", sanitizer.sanitizeLogging(documentId),
                    e.getMessage(), e);
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setErrorMessage(e.getMessage());
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

    private PdfPageRange buildPageRange(Integer startPage, Integer endPage, List<Integer> pages) {
        if (pages != null && !pages.isEmpty()) {
            return new PdfPageRange(pages);
        }
        if (startPage != null || endPage != null) {
            return new PdfPageRange(startPage, endPage);
        }
        return null;
    }
}
