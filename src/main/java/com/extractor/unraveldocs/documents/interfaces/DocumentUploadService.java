package com.extractor.unraveldocs.documents.interfaces;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionUploadData;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentUploadService {
    /**
     * Upload documents with optional collection name and encryption.
     *
     * @param files            The files to upload
     * @param user             The authenticated user
     * @param collectionName   Optional name for the collection (auto-generated if
     *                         null)
     * @param enableEncryption Whether to encrypt files (premium feature)
     * @return Response with upload results
     */
    DocumentCollectionResponse<DocumentCollectionUploadData> uploadDocuments(
            MultipartFile[] files, User user, String collectionName, boolean enableEncryption);
}