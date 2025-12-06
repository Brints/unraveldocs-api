package com.extractor.unraveldocs.documents.interfaces;

public interface DocumentDeleteService {
    void deleteDocument(String collectionId, String userId);
    void deleteFileFromCollection(String collectionId, String documentId, String userId);
}
