package com.extractor.unraveldocs.ocrprocessing.dto.response;

import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import lombok.Data;

import java.util.List;

@Data
public class CollectionResultResponse {
    private String collectionId;
    private DocumentStatus overallStatus;
    private List<FileResultData> files;
}
