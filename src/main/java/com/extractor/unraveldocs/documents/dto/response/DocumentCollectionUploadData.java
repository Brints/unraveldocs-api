package com.extractor.unraveldocs.documents.dto.response;

import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DocumentCollectionUploadData {
    private String collectionId;
    private DocumentStatus overallStatus;
    private List<FileEntryData> files;
}