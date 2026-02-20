package com.extractor.unraveldocs.ocrprocessing.service.impl;

import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.impl.ExtractTextFromDocumentImpl;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.ocrprocessing.utils.ExtractImageURL;
import com.extractor.unraveldocs.ocrprocessing.utils.FindAndValidateFileEntry;
import com.extractor.unraveldocs.storage.service.StorageAllocationService;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractTextFromDocumentImplTest {

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;

    @Mock
    private OcrDataRepository ocrDataRepository;

    @Mock
    private FindAndValidateFileEntry findAndValidateFileEntry;

    @Mock
    private SanitizeLogging sanitizeLogging;

    @Mock
    private StorageAllocationService storageAllocationService;

    @InjectMocks
    private ExtractTextFromDocumentImpl extractTextFromDocumentService;

    private String collectionId;
    private String documentId;
    private String userId;
    private FileEntry fileEntry;

    @BeforeEach
    void setUp() {
        collectionId = UUID.randomUUID().toString();
        documentId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();

        fileEntry = FileEntry.builder()
                .documentId(documentId)
                .fileUrl("https://example.com/image.png")
                .build();

        // Set the value for the @Value annotated field
        ReflectionTestUtils.setField(extractTextFromDocumentService, "tesseractDataPath", "/path/to/tessdata");
    }

    @Test
    void extractTextFromDocument_Success_NewOcrData() throws TesseractException, IOException {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        final List<OcrData> capturedData = new ArrayList<>();
        when(ocrDataRepository.save(any(OcrData.class))).thenAnswer(invocation -> {
            OcrData ocrData = invocation.getArgument(0);

            // Create a copy to capture the state at this moment
            OcrData copy = new OcrData(
                    ocrData.getId(),
                    ocrData.getDocumentId(),
                    ocrData.getStatus(),
                    ocrData.getExtractedText(),
                    ocrData.getErrorMessage(),
                    ocrData.getAiSummary(),
                    ocrData.getDocumentType(),
                    ocrData.getAiTags(),
                    ocrData.getCreatedAt(),
                    ocrData.getUpdatedAt());
            capturedData.add(copy);

            if (ocrData.getStatus() == OcrStatus.COMPLETED) {
                ocrData.setExtractedText("extracted text");
            }
            return ocrData;
        });

        try (MockedStatic<ExtractImageURL> mockedStatic = mockStatic(ExtractImageURL.class)) {
            mockedStatic.when(() -> ExtractImageURL.extractImageURL(any(), any(), anyString()))
                    .thenAnswer(invocation -> {
                        OcrData ocrData = invocation.getArgument(1);
                        ocrData.setStatus(OcrStatus.COMPLETED);
                        ocrData.setExtractedText("extracted text");
                        return null;
                    });

            // Act
            OcrData result = extractTextFromDocumentService.extractTextFromDocument(collectionId, documentId, userId);

            // Assert
            assertNotNull(result);
            assertEquals(OcrStatus.COMPLETED, result.getStatus());
            assertEquals("extracted text", result.getExtractedText());
            assertEquals(documentId, result.getDocumentId());

            verify(ocrDataRepository, times(2)).save(any(OcrData.class));
            assertEquals(2, capturedData.size());
            assertEquals(OcrStatus.PROCESSING, capturedData.get(0).getStatus());
            assertEquals(OcrStatus.COMPLETED, capturedData.get(1).getStatus());

            mockedStatic.verify(() -> ExtractImageURL.extractImageURL(eq(fileEntry), any(OcrData.class), anyString()));
        }
    }

    @Test
    void extractTextFromDocument_Success_OcrDataAlreadyCompleted() {
        // Arrange
        OcrData completedOcrData = new OcrData();
        completedOcrData.setDocumentId(documentId);
        completedOcrData.setStatus(OcrStatus.COMPLETED);
        completedOcrData.setExtractedText("already done");

        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.of(completedOcrData));
        when(sanitizeLogging.sanitizeLogging(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OcrData result = extractTextFromDocumentService.extractTextFromDocument(collectionId, documentId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(OcrStatus.COMPLETED, result.getStatus());
        assertEquals("already done", result.getExtractedText());
        verify(ocrDataRepository, never()).save(any());
    }

    @Test
    void extractTextFromDocument_Failure_TesseractException() throws TesseractException, IOException {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());
        when(ocrDataRepository.save(any(OcrData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<ExtractImageURL> mockedStatic = mockStatic(ExtractImageURL.class)) {
            TesseractException tesseractException = new TesseractException("OCR failed");
            mockedStatic.when(() -> ExtractImageURL.extractImageURL(any(), any(), anyString()))
                    .thenThrow(tesseractException);

            // Act
            extractTextFromDocumentService.extractTextFromDocument(collectionId, documentId, userId);

            // Assert
            ArgumentCaptor<OcrData> ocrDataCaptor = ArgumentCaptor.forClass(OcrData.class);
            verify(ocrDataRepository, times(2)).save(ocrDataCaptor.capture());

            OcrData finalOcrData = ocrDataCaptor.getAllValues().get(1);
            assertEquals(OcrStatus.FAILED, finalOcrData.getStatus());
            assertEquals(tesseractException.getMessage(), finalOcrData.getErrorMessage());
        }
    }

    @Test
    void extractTextFromDocument_Failure_FileEntryNotFound() {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenThrow(new NotFoundException("Document not found"));

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> extractTextFromDocumentService.extractTextFromDocument(collectionId, documentId, userId));

        verify(ocrDataRepository, never()).findByDocumentId(anyString());
        verify(ocrDataRepository, never()).save(any());
    }
}