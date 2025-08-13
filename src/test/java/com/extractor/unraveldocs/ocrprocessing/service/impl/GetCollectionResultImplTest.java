package com.extractor.unraveldocs.ocrprocessing.service.impl;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.ocrprocessing.dto.response.CollectionResultResponse;
import com.extractor.unraveldocs.ocrprocessing.dto.response.FileResultData;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.impl.GetCollectionResultImpl;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCollectionResultImplTest {

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;

    @Mock
    private OcrDataRepository ocrDataRepository;

    @InjectMocks
    private GetCollectionResultImpl getCollectionResultService;

    private String collectionId;
    private DocumentCollection documentCollection;
    private FileEntry fileEntry1;
    private FileEntry fileEntry2;
    private FileEntry fileEntry3;
    private OcrData ocrData1;
    private OcrData ocrData2;

    @BeforeEach
    void setUp() {
        collectionId = UUID.randomUUID().toString();

        fileEntry1 = FileEntry.builder().documentId(UUID.randomUUID().toString()).originalFileName("file1.png").build();
        fileEntry2 = FileEntry.builder().documentId(UUID.randomUUID().toString()).originalFileName("file2.png").build();
        fileEntry3 = FileEntry.builder().documentId(UUID.randomUUID().toString()).originalFileName("file3.png").build();

        documentCollection = new DocumentCollection();
        documentCollection.setId(collectionId);
        documentCollection.setCollectionStatus(DocumentStatus.PROCESSING);
        documentCollection.setFiles(List.of(fileEntry1, fileEntry2, fileEntry3));

        ocrData1 = new OcrData();
        ocrData1.setDocumentId(fileEntry1.getDocumentId());
        ocrData1.setStatus(OcrStatus.COMPLETED);
        ocrData1.setExtractedText("Text for file 1");

        ocrData2 = new OcrData();
        ocrData2.setDocumentId(fileEntry2.getDocumentId());
        ocrData2.setStatus(OcrStatus.FAILED);
        ocrData2.setErrorMessage("OCR failed");
    }

    @Test
    void getCollectionResult_Success() {
        // Arrange
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(documentCollection));
        when(ocrDataRepository.findByDocumentIdIn(anyList())).thenReturn(List.of(ocrData1, ocrData2));

        // Act
        DocumentCollectionResponse<CollectionResultResponse> response = getCollectionResultService.getCollectionResult(collectionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("OCR results retrieved successfully.", response.getMessage());

        CollectionResultResponse data = response.getData();
        assertNotNull(data);
        assertEquals(collectionId, data.getCollectionId());
        assertEquals(DocumentStatus.PROCESSING, data.getOverallStatus());
        assertEquals(3, data.getFiles().size());

        // Assertions for file 1 (COMPLETED)
        FileResultData result1 = data.getFiles().stream().filter(f -> f.getDocumentId().equals(fileEntry1.getDocumentId())).findFirst().orElseThrow();
        assertEquals(OcrStatus.COMPLETED.getStatus(), result1.getStatus());
        assertEquals("Text for file 1", result1.getExtractedText());
        assertNull(result1.getErrorMessage());

        // Assertions for file 2 (FAILED)
        FileResultData result2 = data.getFiles().stream().filter(f -> f.getDocumentId().equals(fileEntry2.getDocumentId())).findFirst().orElseThrow();
        assertEquals(OcrStatus.FAILED.getStatus(), result2.getStatus());
        assertEquals("OCR failed", result2.getErrorMessage());
        assertNull(result2.getExtractedText());

        // Assertions for file 3 (PENDING - no OcrData)
        FileResultData result3 = data.getFiles().stream().filter(f -> f.getDocumentId().equals(fileEntry3.getDocumentId())).findFirst().orElseThrow();
        assertEquals(OcrStatus.PENDING.toString(), result3.getStatus());
        assertNull(result3.getExtractedText());
        assertNull(result3.getErrorMessage());
    }

    @Test
    void getCollectionResult_CollectionNotFound_ThrowsBadRequestException() {
        // Arrange
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.empty());

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            getCollectionResultService.getCollectionResult(collectionId);
        });

        assertEquals("Document collection not found", exception.getMessage());
    }
}