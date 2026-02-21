package com.extractor.unraveldocs.ocrprocessing.interfaces;

import com.extractor.unraveldocs.ocrprocessing.model.OcrData;

import java.util.List;

public interface ExtractTextFromDocumentService {
    OcrData extractTextFromDocument(String collectionId, String documentId, String userId);

    OcrData extractTextFromDocument(String collectionId, String documentId, String userId,
            Integer startPage, Integer endPage, List<Integer> pages);
}
