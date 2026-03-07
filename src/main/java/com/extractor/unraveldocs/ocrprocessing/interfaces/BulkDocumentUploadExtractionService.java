package com.extractor.unraveldocs.ocrprocessing.interfaces;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionUploadData;
import com.extractor.unraveldocs.user.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BulkDocumentUploadExtractionService {
    DocumentCollectionResponse<DocumentCollectionUploadData> uploadDocuments(MultipartFile[] files, User user);

    DocumentCollectionResponse<DocumentCollectionUploadData> uploadDocuments(MultipartFile[] files, User user,
                                                                            Integer startPage, Integer endPage, List<Integer> pages);
}
