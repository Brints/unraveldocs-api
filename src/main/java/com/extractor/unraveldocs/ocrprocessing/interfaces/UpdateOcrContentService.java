package com.extractor.unraveldocs.ocrprocessing.interfaces;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.ocrprocessing.datamodel.ContentFormat;
import com.extractor.unraveldocs.ocrprocessing.dto.response.FileResultData;

public interface UpdateOcrContentService {
    DocumentCollectionResponse<FileResultData> updateOcrContent(
            String collectionId, String documentId, String userId,
            String editedContent, ContentFormat contentFormat);
}
