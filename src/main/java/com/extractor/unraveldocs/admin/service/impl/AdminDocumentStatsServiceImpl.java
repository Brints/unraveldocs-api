package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.DocumentStatsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminDocumentStatsService;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDocumentStatsServiceImpl implements AdminDocumentStatsService {

    private final DocumentCollectionRepository documentCollectionRepository;
    private final ResponseBuilderService responseBuilderService;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<DocumentStatsDto> getDocumentStats() {
        log.info("Fetching admin document statistics");

        long totalCollections = documentCollectionRepository.count();
        long totalFiles = documentCollectionRepository.countTotalFiles();
        long totalStorageBytes = documentCollectionRepository.sumTotalStorageBytes();
        long encryptedDocuments = documentCollectionRepository.countEncryptedDocuments();

        List<Object[]> filesByTypeRaw = documentCollectionRepository.countFilesByType();
        Map<String, Long> filesByType = new LinkedHashMap<>();
        for (Object[] row : filesByTypeRaw) {
            filesByType.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<Object[]> filesByStatusRaw = documentCollectionRepository.countFilesByStatus();
        Map<String, Long> filesByStatus = new LinkedHashMap<>();
        long successCount = 0;
        for (Object[] row : filesByStatusRaw) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            filesByStatus.put(status, count);
            if ("COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                successCount += count;
            }
        }

        double averageFilesPerCollection = totalCollections == 0 ? 0.0 : (double) totalFiles / totalCollections;
        double uploadSuccessRate = totalFiles == 0 ? 0.0 : ((double) successCount / totalFiles) * 100.0;

        DocumentStatsDto stats = DocumentStatsDto.builder()
                .totalCollections(totalCollections)
                .totalFiles(totalFiles)
                .totalStorageBytes(totalStorageBytes)
                .filesByType(filesByType)
                .filesByStatus(filesByStatus)
                .encryptedDocuments(encryptedDocuments)
                .averageFilesPerCollection(averageFilesPerCollection)
                .uploadSuccessRate(uploadSuccessRate)
                .build();

        return responseBuilderService.buildUserResponse(
                stats,
                HttpStatus.OK,
                "Document stats retrieved successfully"
        );
    }
}
