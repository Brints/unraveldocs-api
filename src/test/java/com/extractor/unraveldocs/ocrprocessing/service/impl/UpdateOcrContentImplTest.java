package com.extractor.unraveldocs.ocrprocessing.service.impl;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchIndexingService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.ocrprocessing.datamodel.ContentFormat;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.dto.response.FileResultData;
import com.extractor.unraveldocs.ocrprocessing.impl.UpdateOcrContentImpl;
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
class UpdateOcrContentImplTest {

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;

    @Mock
    private OcrDataRepository ocrDataRepository;

    @Mock
    private FindAndValidateFileEntry findAndValidateFileEntry;

    @Mock
    private Optional<ElasticsearchIndexingService> elasticsearchIndexingService;

    @InjectMocks
    private UpdateOcrContentImpl updateOcrContentService;

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
                .originalFileName("test-document.pdf")
                .build();

        ocrData = new OcrData();
        ocrData.setId(UUID.randomUUID().toString());
        ocrData.setDocumentId(documentId);
        ocrData.setStatus(OcrStatus.COMPLETED);
        ocrData.setExtractedText("Original OCR extracted text");
        ocrData.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void updateOcrContent_HtmlFormat_Success() {
        // Arrange
        String htmlContent = "<p>Edited <b>bold</b> text with <a href=\"https://example.com\">link</a></p>";
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.of(ocrData));
        when(ocrDataRepository.save(any(OcrData.class))).thenReturn(ocrData);

        // Act
        DocumentCollectionResponse<FileResultData> response = updateOcrContentService
                .updateOcrContent(collectionId, documentId, userId, htmlContent, ContentFormat.HTML);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("Document content updated successfully.", response.getMessage());

        FileResultData data = response.getData();
        assertNotNull(data);
        assertEquals(documentId, data.getDocumentId());
        assertEquals("Original OCR extracted text", data.getExtractedText());
        assertNotNull(data.getEditedContent());
        assertEquals("HTML", data.getContentFormat());
        assertEquals(userId, data.getEditedBy());
        assertNotNull(data.getEditedAt());

        verify(ocrDataRepository).save(any(OcrData.class));
    }

    @Test
    void updateOcrContent_MarkdownFormat_Success() {
        // Arrange
        String markdownContent = "# Heading\n\n**Bold text** with [link](https://example.com)";
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.of(ocrData));
        when(ocrDataRepository.save(any(OcrData.class))).thenReturn(ocrData);

        // Act
        DocumentCollectionResponse<FileResultData> response = updateOcrContentService
                .updateOcrContent(collectionId, documentId, userId, markdownContent, ContentFormat.MARKDOWN);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());

        FileResultData data = response.getData();
        assertNotNull(data);
        assertEquals(markdownContent, data.getEditedContent());
        assertEquals("MARKDOWN", data.getContentFormat());

        verify(ocrDataRepository).save(any(OcrData.class));
    }

    @Test
    void updateOcrContent_HtmlSanitization_StripsScriptTags() {
        // Arrange
        String maliciousContent = "<p>Text</p><script>alert('xss')</script><b>Bold</b>";
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.of(ocrData));
        when(ocrDataRepository.save(any(OcrData.class))).thenReturn(ocrData);

        // Act
        DocumentCollectionResponse<FileResultData> response = updateOcrContentService
                .updateOcrContent(collectionId, documentId, userId, maliciousContent, ContentFormat.HTML);

        // Assert
        FileResultData data = response.getData();
        assertNotNull(data);
        assertFalse(data.getEditedContent().contains("<script>"));
        assertTrue(data.getEditedContent().contains("<b>Bold</b>"));

        verify(ocrDataRepository).save(any(OcrData.class));
    }

    @Test
    void updateOcrContent_OcrNotCompleted_ThrowsBadRequestException() {
        // Arrange
        ocrData.setStatus(OcrStatus.PROCESSING);
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.of(ocrData));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> updateOcrContentService
                .updateOcrContent(collectionId, documentId, userId, "content", ContentFormat.HTML));

        assertTrue(exception.getMessage().contains("not completed OCR processing"));
        verify(ocrDataRepository, never()).save(any(OcrData.class));
    }

    @Test
    void updateOcrContent_OcrDataNotFound_ThrowsNotFoundException() {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> updateOcrContentService
                .updateOcrContent(collectionId, documentId, userId, "content", ContentFormat.HTML));

        assertEquals("OCR data not found for document ID: " + documentId, exception.getMessage());
        verify(ocrDataRepository, never()).save(any(OcrData.class));
    }

    @Test
    void updateOcrContent_FileEntryNotFound_ThrowsNotFoundException() {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenThrow(new NotFoundException("Document not found"));

        // Act & Assert
        assertThrows(NotFoundException.class, () -> updateOcrContentService.updateOcrContent(collectionId, documentId,
                userId, "content", ContentFormat.HTML));

        verify(ocrDataRepository, never()).findByDocumentId(anyString());
        verify(ocrDataRepository, never()).save(any(OcrData.class));
    }

    @Test
    void updateOcrContent_UserForbidden_ThrowsForbiddenException() {
        // Arrange
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenThrow(new ForbiddenException("Access denied"));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> updateOcrContentService.updateOcrContent(collectionId, documentId,
                userId, "content", ContentFormat.HTML));

        verify(ocrDataRepository, never()).findByDocumentId(anyString());
        verify(ocrDataRepository, never()).save(any(OcrData.class));
    }

    @Test
    void updateOcrContent_PreservesOriginalExtractedText() {
        // Arrange
        String editedContent = "<p>User corrected text</p>";
        when(findAndValidateFileEntry.findAndValidateFileEntry(collectionId, documentId, userId,
                documentCollectionRepository))
                .thenReturn(fileEntry);
        when(ocrDataRepository.findByDocumentId(documentId)).thenReturn(Optional.of(ocrData));
        when(ocrDataRepository.save(any(OcrData.class))).thenReturn(ocrData);

        // Act
        DocumentCollectionResponse<FileResultData> response = updateOcrContentService
                .updateOcrContent(collectionId, documentId, userId, editedContent, ContentFormat.HTML);

        // Assert - original text should be preserved
        FileResultData data = response.getData();
        assertEquals("Original OCR extracted text", data.getExtractedText());
        assertNotNull(data.getEditedContent());
    }
}
