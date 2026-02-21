package com.extractor.unraveldocs.ocrprocessing.service;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionUploadData;
import com.extractor.unraveldocs.ocrprocessing.datamodel.ContentFormat;
import com.extractor.unraveldocs.ocrprocessing.dto.response.CollectionResultResponse;
import com.extractor.unraveldocs.ocrprocessing.dto.response.FileResultData;
import com.extractor.unraveldocs.ocrprocessing.interfaces.*;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OcrService {
    private final ExtractTextFromDocumentService extractTextFromDocumentService;
    private final GetOcrDataService getOcrDataService;
    private final GetCollectionResultService getCollectionResultService;
    private final BulkDocumentUploadExtractionService extractionService;
    private final UpdateOcrContentService updateOcrContentService;

    public OcrData extractTextFromDocument(String collectionId, String documentId, String userId,
            Integer startPage, Integer endPage, java.util.List<Integer> pages) {
        return extractTextFromDocumentService.extractTextFromDocument(collectionId, documentId, userId,
                startPage, endPage, pages);
    }

    public DocumentCollectionResponse<FileResultData> getOcrData(String collectionId, String documentId,
            String userId) {
        return getOcrDataService.getOcrData(collectionId, documentId, userId);
    }

    public DocumentCollectionResponse<CollectionResultResponse> getCollectionResult(String collectionId) {
        return getCollectionResultService.getCollectionResult(collectionId);
    }

    public DocumentCollectionResponse<DocumentCollectionUploadData> uploadDocuments(MultipartFile[] files, User user) {
        return extractionService.uploadDocuments(files, user);
    }

    public DocumentCollectionResponse<FileResultData> updateOcrContent(
            String collectionId, String documentId, String userId,
            String editedContent, ContentFormat contentFormat) {
        return updateOcrContentService.updateOcrContent(collectionId, documentId, userId,
                editedContent, contentFormat);
    }
}
