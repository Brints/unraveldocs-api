package com.extractor.unraveldocs.ocrprocessing.service;

import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.interfaces.ProcessOcrService;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.extractor.unraveldocs.ocrprocessing.utils.ExtractImageURL.extractImageURL;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessOcr implements ProcessOcrService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final SanitizeLogging sanitizeLogging;
    private final OcrDataRepository ocrDataRepository;

    @Value("${tesseract.datapath}")
    private String tesseractDataPath;

    @Override
    @Transactional
    public void processOcrRequest(String collectionId, String documentId) {
        DocumentCollection collection = documentCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new NotFoundException("Collection not found with ID: " + collectionId));

        FileEntry fileEntry = collection.getFiles().stream()
                .filter(f -> f.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("File not found with document ID: " + documentId));

        OcrData ocrData = ocrDataRepository.findByDocumentId(fileEntry.getDocumentId())
                .orElseThrow(() -> new NotFoundException("OCR data not found for document ID: " + fileEntry.getDocumentId()));

        if (ocrData.getStatus() == OcrStatus.COMPLETED) {
            log.info("File {} already processed. Skipping.", sanitizeLogging.sanitizeLogging(documentId));
            return;
        }
        ocrData.setStatus(OcrStatus.PROCESSING);
        ocrDataRepository.save(ocrData);

        try {
            log.info("Starting OCR text extraction for document: {}", sanitizeLogging.sanitizeLogging(documentId));
            extractImageURL(fileEntry, ocrData, tesseractDataPath);
            log.info("OCR text extraction completed for document: {}",
                    sanitizeLogging.sanitizeLogging(documentId));
        } catch (IOException | TesseractException e) {
            log.error("OCR processing failed for document {}: {}",
                    sanitizeLogging.sanitizeLogging(documentId), e.getMessage(), e);
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setErrorMessage(e.getMessage());
        } finally {
            updateCollectionStatus(collection);
            ocrDataRepository.save(ocrData);
            documentCollectionRepository.save(collection);
        }
    }

    private void updateCollectionStatus(DocumentCollection collection) {
        List<String> documentIds = collection.getFiles().stream()
                .map(FileEntry::getDocumentId)
                .toList();

        if (documentIds.isEmpty()) {
            collection.setCollectionStatus(DocumentStatus.PROCESSED);
            return;
        }

        Map<String, OcrStatus> statusMap = ocrDataRepository.findByDocumentIdIn(documentIds).stream()
                .collect(Collectors.toMap(OcrData::getDocumentId, OcrData::getStatus));

        long totalFiles = documentIds.size();

        long completedCount = documentIds.stream()
                .filter(id -> statusMap.get(id) == OcrStatus.COMPLETED)
                .count();

        long failedCount = documentIds.stream()
                .filter(id -> statusMap.get(id) == OcrStatus.FAILED)
                .count();

        if (completedCount == totalFiles) {
            collection.setCollectionStatus(DocumentStatus.PROCESSED);
        } else if (completedCount + failedCount == totalFiles) {
            collection.setCollectionStatus(DocumentStatus.FAILED_OCR);
        } else {
            collection.setCollectionStatus(DocumentStatus.PROCESSING);
        }
        log.info("Collection {} status updated to: {}",
                sanitizeLogging.sanitizeLogging(collection.getId()), collection.getCollectionStatus());
    }
}
