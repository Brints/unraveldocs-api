package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.DocumentStatsDto;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDocumentStatsServiceImplTest {

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AdminDocumentStatsServiceImpl adminDocumentStatsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getDocumentStats_Success() {
        // Arrange
        when(documentCollectionRepository.count()).thenReturn(100L);
        when(documentCollectionRepository.countTotalFiles()).thenReturn(500L);
        when(documentCollectionRepository.sumTotalStorageBytes()).thenReturn(1024000L);
        when(documentCollectionRepository.countEncryptedDocuments()).thenReturn(50L);

        List<Object[]> filesByTypeRaw = new ArrayList<>();
        filesByTypeRaw.add(new Object[]{"PDF", 400L});
        filesByTypeRaw.add(new Object[]{"JPG", 100L});
        when(documentCollectionRepository.countFilesByType()).thenReturn(filesByTypeRaw);

        List<Object[]> filesByStatusRaw = new ArrayList<>();
        filesByStatusRaw.add(new Object[]{"COMPLETED", 450L});
        filesByStatusRaw.add(new Object[]{"FAILED", 50L});
        when(documentCollectionRepository.countFilesByStatus()).thenReturn(filesByStatusRaw);

        DocumentStatsDto expectedStats = DocumentStatsDto.builder()
                .totalCollections(100L)
                .totalFiles(500L)
                .totalStorageBytes(1024000L)
                .encryptedDocuments(50L)
                .filesByType(Map.of("PDF", 400L, "JPG", 100L))
                .filesByStatus(Map.of("COMPLETED", 450L, "FAILED", 50L))
                .averageFilesPerCollection(5.0)
                .uploadSuccessRate(90.0)
                .build();

        UnravelDocsResponse<DocumentStatsDto> expectedResponse = new UnravelDocsResponse<>(
                200,
                "success",
                "Document stats retrieved successfully",
                expectedStats
        );

        when(responseBuilderService.buildUserResponse(
                any(DocumentStatsDto.class),
                eq(HttpStatus.OK),
                eq("Document stats retrieved successfully")
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsResponse<DocumentStatsDto> response = adminDocumentStatsService.getDocumentStats();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("success", response.getStatus());

        DocumentStatsDto data = response.getData();
        assertNotNull(data);
        assertEquals(100L, data.getTotalCollections());
        assertEquals(500L, data.getTotalFiles());
        assertEquals(1024000L, data.getTotalStorageBytes());
        assertEquals(50L, data.getEncryptedDocuments());
        assertEquals(5.0, data.getAverageFilesPerCollection());
        assertEquals(90.0, data.getUploadSuccessRate());
        assertEquals(400L, data.getFilesByType().get("PDF"));
        assertEquals(50L, data.getFilesByStatus().get("FAILED"));
    }
}
