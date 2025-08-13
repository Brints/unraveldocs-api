package com.extractor.unraveldocs.ocrprocessing.service.impl;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.ocrprocessing.dto.response.FileResultData;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.impl.GetOcrDataImpl;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.ocrprocessing.utils.FindAndValidateFileEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetOcrDataImplTest {

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;

    @Mock
    private OcrDataRepository ocrDataRepository;

    @Mock
    private FindAndValidateFileEntry findAndValidateFileEntry;

    @InjectMocks
    private GetOcrDataImpl getOcrDataService;

    private String collectionId;
    private String documentId;
    private String userId;
    private FileEntry fileEntry;
    private OcrData ocrData;

    @BeforeEach
    void setUp() {
        collectionId = UUID.randomUUID().toString();
        documentId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();

        fileEntry = FileEntry.builder()
                .documentId(documentId)
                .originalFileName("test.png")
                .build();

        ocrData = new OcrData();
        ocrData.setDocumentId(documentId);
        ocrData.setStatus(OcrStatus.COMPLETED);
        ocrData.setExtractedText("Sample extracted text");
        ocrData.setErrorMessage(null);
        ocrData.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void getOcrData_Success() {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId, documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.of(ocrData));

        // Act
        DocumentCollectionResponse<FileResultData> response = getOcrDataService.getOcrData(collectionId, documentId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("OCR data retrieved successfully.", response.getMessage());

        FileResultData data = response.getData();
        assertNotNull(data);
        assertEquals(documentId, data.getDocumentId());
        assertEquals(fileEntry.getOriginalFileName(), data.getOriginalFileName());
        assertEquals(OcrStatus.COMPLETED.name(), data.getStatus());
        assertEquals(ocrData.getExtractedText(), data.getExtractedText());
        assertNull(data.getErrorMessage());

        verify(findAndValidateFileEntry).findAndValidateFileEntry(collectionId, documentId, userId, documentCollectionRepository);
        verify(ocrDataRepository).findByDocumentId(documentId);
    }

    @Test
    void getOcrData_OcrDataNotFound_ThrowsNotFoundException() {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId, documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                getOcrDataService.getOcrData(collectionId, documentId, userId)
        );

        assertEquals("OCR data not found for document ID: " + documentId, exception.getMessage());
    }

    @Test
    void getOcrData_FileEntryNotFound_ThrowsNotFoundException() {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId, documentCollectionRepository))
                .thenThrow(new NotFoundException("Document not found"));

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
                getOcrDataService.getOcrData(collectionId, documentId, userId)
        );

        verify(ocrDataRepository, never()).findByDocumentId(anyString());
    }

    @Test
    void getOcrData_UserForbidden_ThrowsForbiddenException() {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId, documentCollectionRepository))
                .thenThrow(new ForbiddenException("Access denied"));

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
                getOcrDataService.getOcrData(collectionId, documentId, userId)
        );

        verify(ocrDataRepository, never()).findByDocumentId(anyString());
    }
}