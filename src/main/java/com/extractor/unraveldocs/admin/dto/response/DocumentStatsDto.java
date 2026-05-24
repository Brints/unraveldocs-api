package com.extractor.unraveldocs.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatsDto {
    private long totalCollections;
    private long totalFiles;
    private long totalStorageBytes;
    private Map<String, Long> filesByType;
    private Map<String, Long> filesByStatus;
    private long encryptedDocuments;
    private double averageFilesPerCollection;
    private double uploadSuccessRate;
}
