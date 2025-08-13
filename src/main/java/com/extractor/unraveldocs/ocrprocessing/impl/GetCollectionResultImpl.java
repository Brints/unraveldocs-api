package com.extractor.unraveldocs.ocrprocessing.impl;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.ocrprocessing.dto.response.CollectionResultResponse;
import com.extractor.unraveldocs.ocrprocessing.dto.response.FileResultData;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.interfaces.GetCollectionResultService;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetCollectionResultImpl implements GetCollectionResultService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final OcrDataRepository ocrDataRepository;

    @Override
    @Transactional(readOnly = true)
    public DocumentCollectionResponse<CollectionResultResponse> getCollectionResult(String collectionId) {
        DocumentCollection documentCollection = documentCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new BadRequestException("Document collection not found"));

        List<String> documentIds = documentCollection.getFiles().stream()
                .map(FileEntry::getDocumentId)
                .toList();

        Map<String, OcrData> ocrDataMap = ocrDataRepository.findByDocumentIdIn(
                        documentIds).stream()
                .collect(Collectors.toMap(OcrData::getDocumentId, ocrData -> ocrData));

        List<FileResultData> fileResults = documentCollection.getFiles().stream()
                .map(fileEntry -> {
                    OcrData ocrData = ocrDataMap.get(fileEntry.getDocumentId());

                    FileResultData fileResultData = new FileResultData();
                    fileResultData.setDocumentId(fileEntry.getDocumentId());
                    fileResultData.setOriginalFileName(fileEntry.getOriginalFileName());
                    fileResultData.setStatus(ocrData != null ? ocrData.getStatus().getStatus() : OcrStatus.PENDING.toString());
                    fileResultData.setErrorMessage(ocrData != null ? ocrData.getErrorMessage() : null);
                    fileResultData.setCreatedAt(ocrData != null ? ocrData.getCreatedAt() : null);
                    fileResultData.setExtractedText(ocrData != null ? ocrData.getExtractedText() : null);
                    return fileResultData;
                }).toList();

        CollectionResultResponse response = new CollectionResultResponse();
        response.setCollectionId(collectionId);
        response.setOverallStatus(documentCollection.getCollectionStatus());
        response.setFiles(fileResults);

        return DocumentCollectionResponse.<CollectionResultResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .status("success")
                .message("OCR results retrieved successfully.")
                .data(response)
                .build();
    }
}
