package com.extractor.unraveldocs.documents.interfaces;

import com.extractor.unraveldocs.documents.dto.request.UpdateCollectionRequest;
import com.extractor.unraveldocs.documents.dto.request.UpdateDocumentRequest;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.FileEntryData;
import com.extractor.unraveldocs.documents.dto.response.GetDocumentCollectionData;

/**
 * Service interface for updating collection and document names.
 */
public interface CollectionUpdateService {

    /**
     * Updates the name of a document collection.
     *
     * @param collectionId The collection ID
     * @param request      The update request containing the new name
     * @param userId       The authenticated user's ID
     * @return Response containing the updated collection data
     */
    DocumentCollectionResponse<GetDocumentCollectionData> updateCollectionName(
            String collectionId, UpdateCollectionRequest request, String userId);

    /**
     * Updates the display name of a document within a collection.
     *
     * @param collectionId The collection ID
     * @param documentId   The document ID
     * @param request      The update request containing the new display name
     * @param userId       The authenticated user's ID
     * @return Response containing the updated file entry data
     */
    DocumentCollectionResponse<FileEntryData> updateDocumentName(
            String collectionId, String documentId, UpdateDocumentRequest request, String userId);
}
