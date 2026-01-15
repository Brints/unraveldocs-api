package com.extractor.unraveldocs.documents.dto.response;

import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
public class DocumentCollectionSummary {
    private String id;
    private String name;
    private DocumentStatus collectionStatus;
    private int fileCount;
    private boolean hasEncryptedFiles;
    private OffsetDateTime uploadTimestamp;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}