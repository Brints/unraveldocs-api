package com.extractor.unraveldocs.ocrprocessing.impl;

import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.interfaces.ExtractTextFromDocumentService;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.ocrprocessing.utils.FindAndValidateFileEntry;
import com.extractor.unraveldocs.storage.service.StorageAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;

import static com.extractor.unraveldocs.ocrprocessing.utils.ExtractImageURL.extractImageURL;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractTextFromDocumentImpl implements ExtractTextFromDocumentService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final OcrDataRepository ocrDataRepository;
    private final FindAndValidateFileEntry validateFileEntry;
    private final SanitizeLogging sanitizeLogging;
    private final StorageAllocationService storageAllocationService;

    @Value("${tesseract.datapath}")
    private String tesseractDataPath;

    @Override
    @Transactional
    public OcrData extractTextFromDocument(String collectionId, String documentId, String userId) {
        FileEntry fileEntry = validateFileEntry
                .findAndValidateFileEntry(collectionId, documentId, userId, documentCollectionRepository);

        Optional<OcrData> existingOcrData = ocrDataRepository.findByDocumentId(documentId);
        if (existingOcrData.isPresent()) {
            log.info("OCR data already exists for document ID: {}", sanitizeLogging.sanitizeLogging(documentId));
            return existingOcrData.get();
        }

        OcrData ocrData = new OcrData();
        ocrData.setDocumentId(documentId);
        try {
            log.info("Starting OCR text extraction for document: {}", sanitizeLogging.sanitizeLogging(documentId));
            ocrData.setStatus(OcrStatus.PROCESSING);
            ocrDataRepository.save(ocrData);

            extractImageURL(fileEntry, ocrData, tesseractDataPath);
            log.info("OCR text extraction completed for document: {}", sanitizeLogging.sanitizeLogging(documentId));

            // Update OCR pages used (for monthly quota tracking)
            storageAllocationService.updateOcrUsage(userId, 1);
        } catch (IOException | TesseractException e) {
            log.error("OCR processing failed for document {}: {}", sanitizeLogging.sanitizeLogging(documentId), e.getMessage(), e);
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("An unexpected error occurred while processing document {}: {}", sanitizeLogging.sanitizeLogging(documentId), e.getMessage(), e);
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setErrorMessage("An unexpected error occurred: " + e.getMessage());
            throw new ServiceException("Failed to process OCR for document: " + sanitizeLogging.sanitizeLogging(documentId), e);
        }

        return ocrDataRepository.save(ocrData);
    }
}
