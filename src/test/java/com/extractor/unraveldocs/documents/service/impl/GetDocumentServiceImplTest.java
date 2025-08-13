package com.extractor.unraveldocs.documents.service.impl;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionSummary;
import com.extractor.unraveldocs.documents.dto.response.FileEntryData;
import com.extractor.unraveldocs.documents.dto.response.GetDocumentCollectionData;
import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import com.extractor.unraveldocs.documents.impl.GetDocumentServiceImpl;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetDocumentServiceImplTest {

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;

    @InjectMocks
    private GetDocumentServiceImpl getDocumentService;

    private String userId;
    private String collectionId;
    private DocumentCollection testCollection;
    private FileEntry testFileEntry;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        collectionId = UUID.randomUUID().toString();

        User testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.USER);
        testUser.setVerified(true);
        testUser.setActive(true);

        testFileEntry = FileEntry.builder()
                .documentId(UUID.randomUUID().toString())
                .originalFileName("test.pdf")
                .fileSize(1024L)
                .fileUrl("https://example.com/test.pdf")
                .uploadStatus("SUCCESS")
                .build();

        testCollection = DocumentCollection.builder()
                .id(collectionId)
                .user(testUser)
                .collectionStatus(DocumentStatus.COMPLETED)
                .uploadTimestamp(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .files(new ArrayList<>(List.of(testFileEntry)))
                .build();
    }

    @Test
    void getDocumentCollectionById_success() {
        // Arrange
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(testCollection));

        // Act
        DocumentCollectionResponse<GetDocumentCollectionData> response = getDocumentService.getDocumentCollectionById(collectionId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("Document collection retrieved successfully.", response.getMessage());

        GetDocumentCollectionData data = response.getData();
        assertNotNull(data);
        assertEquals(collectionId, data.getId());
        assertEquals(userId, data.getUserId());
        assertEquals(1, data.getFiles().size());
        assertEquals(testFileEntry.getDocumentId(), data.getFiles().getFirst().getDocumentId());

        verify(documentCollectionRepository).findById(collectionId);
    }

    @Test
    void getDocumentCollectionById_notFound() {
        // Arrange
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> getDocumentService.getDocumentCollectionById(collectionId, userId));
        assertEquals("Document collection not found with ID: " + collectionId, exception.getMessage());
    }

    @Test
    void getDocumentCollectionById_forbidden() {
        // Arrange
        String anotherUserId = UUID.randomUUID().toString();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(testCollection));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> getDocumentService.getDocumentCollectionById(collectionId, anotherUserId));
        assertEquals("You are not authorized to view this document collection.", exception.getMessage());
    }

    @Test
    void getAllDocumentCollectionsByUser_success() {
        // Arrange
        when(documentCollectionRepository.findAllByUserId(userId)).thenReturn(List.of(testCollection));

        // Act
        DocumentCollectionResponse<List<DocumentCollectionSummary>> response = getDocumentService.getAllDocumentCollectionsByUser(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("Document collections retrieved successfully.", response.getMessage());

        List<DocumentCollectionSummary> summaries = response.getData();
        assertNotNull(summaries);
        assertEquals(1, summaries.size());
        assertEquals(collectionId, summaries.getFirst().getId());
        assertEquals(1, summaries.getFirst().getFileCount());

        verify(documentCollectionRepository).findAllByUserId(userId);
    }

    @Test
    void getAllDocumentCollectionsByUser_emptyList() {
        // Arrange
        when(documentCollectionRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        // Act
        DocumentCollectionResponse<List<DocumentCollectionSummary>> response = getDocumentService.getAllDocumentCollectionsByUser(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    void getFileFromCollection_success() {
        // Arrange
        String documentId = testFileEntry.getDocumentId();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(testCollection));

        // Act
        DocumentCollectionResponse<FileEntryData> response = getDocumentService.getFileFromCollection(collectionId, documentId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("success", response.getStatus());
        assertEquals("File retrieved successfully.", response.getMessage());

        FileEntryData data = response.getData();
        assertNotNull(data);
        assertEquals(documentId, data.getDocumentId());
        assertEquals(testFileEntry.getOriginalFileName(), data.getOriginalFileName());

        verify(documentCollectionRepository).findById(collectionId);
    }

    @Test
    void getFileFromCollection_collectionNotFound() {
        // Arrange
        String documentId = testFileEntry.getDocumentId();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> getDocumentService.getFileFromCollection(collectionId, documentId, userId));
        assertEquals("Document collection not found with ID: " + collectionId, exception.getMessage());
    }

    @Test
    void getFileFromCollection_fileNotFoundInCollection() {
        // Arrange
        String wrongDocumentId = UUID.randomUUID().toString();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(testCollection));

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> getDocumentService.getFileFromCollection(collectionId, wrongDocumentId, userId));
        assertEquals("File with document ID: " + wrongDocumentId + " not found in collection: " + collectionId, exception.getMessage());
    }

    @Test
    void getFileFromCollection_forbidden() {
        // Arrange
        String documentId = testFileEntry.getDocumentId();
        String anotherUserId = UUID.randomUUID().toString();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(testCollection));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> getDocumentService.getFileFromCollection(collectionId, documentId, anotherUserId));
        assertEquals("You are not authorized to access this document collection.", exception.getMessage());
    }
}