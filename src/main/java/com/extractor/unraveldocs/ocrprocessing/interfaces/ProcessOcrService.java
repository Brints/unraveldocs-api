package com.extractor.unraveldocs.ocrprocessing.interfaces;

import java.util.List;

public interface ProcessOcrService {
    void processOcrRequest(String collectionId, String documentId);

    void processOcrRequest(String collectionId, String documentId,
                           Integer startPage, Integer endPage, List<Integer> pages);
}
