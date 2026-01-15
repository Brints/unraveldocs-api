package com.extractor.unraveldocs.documents.interfaces;

import com.extractor.unraveldocs.documents.dto.request.MoveDocumentRequest;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.FileEntryData;

/**
 * Service interface for moving documents between collections.
 * This is a premium feature restricted to Starter+ subscriptions.
 */
public interface DocumentMoveService {

    /**
     * Moves a document from one collection to another.
     *
     * @param request The move request containing source/target collection IDs and
     *                document ID
     * @param userId  The authenticated user's ID
     * @return Response containing the moved file's data
     */
    DocumentCollectionResponse<FileEntryData> moveDocument(MoveDocumentRequest request, String userId);
}
