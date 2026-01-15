package com.extractor.unraveldocs.documents.service;

import com.extractor.unraveldocs.documents.dto.request.MoveDocumentRequest;
import com.extractor.unraveldocs.documents.dto.request.UpdateCollectionRequest;
import com.extractor.unraveldocs.documents.dto.request.UpdateDocumentRequest;
import com.extractor.unraveldocs.documents.dto.response.*;
import com.extractor.unraveldocs.documents.interfaces.ClearAllCollectionsService;
import com.extractor.unraveldocs.documents.interfaces.CollectionUpdateService;
import com.extractor.unraveldocs.documents.interfaces.DocumentDeleteService;
import com.extractor.unraveldocs.documents.interfaces.DocumentMoveService;
import com.extractor.unraveldocs.documents.interfaces.DocumentUploadService;
import com.extractor.unraveldocs.documents.interfaces.GetDocumentService;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final ClearAllCollectionsService clearAllCollectionsService;
    private final DocumentDeleteService documentDeleteService;
    private final DocumentUploadService documentUploadService;
    private final GetDocumentService getDocumentService;
    private final DocumentMoveService documentMoveService;
    private final CollectionUpdateService collectionUpdateService;

    public DocumentCollectionResponse<DocumentCollectionUploadData> uploadDocuments(
            MultipartFile[] files, User user, String collectionName, boolean enableEncryption) {
        return documentUploadService.uploadDocuments(files, user, collectionName, enableEncryption);
    }

    // Keep backward compatible method
    public DocumentCollectionResponse<DocumentCollectionUploadData> uploadDocuments(MultipartFile[] files, User user) {
        return documentUploadService.uploadDocuments(files, user, null, false);
    }

    public void deleteDocument(String collectionId, String userId) {
        documentDeleteService.deleteDocument(collectionId, userId);
    }

    public void deleteFileFromCollection(String collectionId, String documentId, String userId) {
        documentDeleteService.deleteFileFromCollection(collectionId, documentId, userId);
    }

    public DocumentCollectionResponse<GetDocumentCollectionData> getDocumentCollectionById(String collectionId,
            String userId) {
        return getDocumentService.getDocumentCollectionById(collectionId, userId);
    }

    public DocumentCollectionResponse<List<DocumentCollectionSummary>> getAllDocumentCollectionsByUser(String userId) {
        return getDocumentService.getAllDocumentCollectionsByUser(userId);
    }

    public DocumentCollectionResponse<FileEntryData> getFileFromCollection(String collectionId, String documentId,
            String userId) {
        return getDocumentService.getFileFromCollection(collectionId, documentId, userId);
    }

    public void clearAllCollections(String userId) {
        clearAllCollectionsService.clearAllCollections(userId);
    }

    // document moving and name updates
    public DocumentCollectionResponse<FileEntryData> moveDocument(MoveDocumentRequest request, String userId) {
        return documentMoveService.moveDocument(request, userId);
    }

    public DocumentCollectionResponse<GetDocumentCollectionData> updateCollectionName(
            String collectionId, UpdateCollectionRequest request, String userId) {
        return collectionUpdateService.updateCollectionName(collectionId, request, userId);
    }

    public DocumentCollectionResponse<FileEntryData> updateDocumentName(
            String collectionId, String documentId, UpdateDocumentRequest request, String userId) {
        return collectionUpdateService.updateDocumentName(collectionId, documentId, request, userId);
    }
}