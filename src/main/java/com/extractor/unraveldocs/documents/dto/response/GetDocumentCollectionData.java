package com.extractor.unraveldocs.documents.dto.response;

import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class GetDocumentCollectionData {
    private String id;
    private String userId;
    private DocumentStatus collectionStatus;
    private OffsetDateTime uploadTimestamp;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<FileEntryData> files;
}