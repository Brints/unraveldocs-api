package com.extractor.unraveldocs.documents.service.impl;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.documents.enums.DocumentStatus;
import com.extractor.unraveldocs.documents.enums.DocumentUploadState;
import com.extractor.unraveldocs.documents.impl.DocumentDeleteImpl;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.utils.imageupload.aws.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentDeleteImplTest {
    @Mock
    private AwsS3Service awsS3Service;

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;

    @Mock
    private SanitizeLogging s;

    @InjectMocks
    private DocumentDeleteImpl documentDeleteService;

    private User testUser;


    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.USER);
        testUser.setVerified(true);
        testUser.setActive(true);

    }

    @Test
    void deleteDocument_success() {
        // Arrange
        when(s.sanitizeLogging(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        String collectionId = UUID.randomUUID().toString();
        String file1StorageId = "storageId1";
        String file1DocumentId = UUID.randomUUID().toString();
        String file1Url = "https://s3.amazonaws.com/bucket/storageId1";

        FileEntry fileEntry1 = FileEntry.builder().documentId(file1DocumentId).storageId(file1StorageId).fileUrl(file1Url).uploadStatus("SUCCESS").build();
        DocumentCollection collection = DocumentCollection.builder()
                .id(collectionId)
                .user(testUser)
                .files(new ArrayList<>(List.of(fileEntry1)))
                .collectionStatus(DocumentStatus.COMPLETED)
                .build();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));
        doNothing().when(awsS3Service).deleteFile(file1Url);
        doNothing().when(documentCollectionRepository).delete(collection);

        // Act
        documentDeleteService.deleteDocument(collectionId, testUser.getId());

        // Assert
        verify(documentCollectionRepository).findById(collectionId);
        verify(awsS3Service).deleteFile(file1Url);
        verify(documentCollectionRepository).delete(collection);

        verify(s).sanitizeLogging(file1StorageId);
        verify(s).sanitizeLogging(file1DocumentId);
        verify(s, times(2)).sanitizeLogging(collectionId);
    }

    @Test
    void deleteDocument_collectionNotFound() {
        // Arrange
        String collectionId = UUID.randomUUID().toString();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> documentDeleteService.deleteDocument(collectionId, testUser.getId()));
        assertEquals("Document collection not found with ID: " + collectionId, exception.getMessage());
        verify(awsS3Service, never()).deleteFile(anyString());
        verify(documentCollectionRepository, never()).delete(any(DocumentCollection.class));
        verifyNoInteractions(s);
    }

    @Test
    void deleteDocument_forbidden() {
        // Arrange
        String collectionId = UUID.randomUUID().toString();
        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID().toString());


        DocumentCollection collection = DocumentCollection.builder().id(collectionId).user(anotherUser).files(new ArrayList<>()).build();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                documentDeleteService.deleteDocument(collectionId, testUser.getId()));

        assertEquals("You are not authorized to delete this document collection.", exception.getMessage());
        verify(awsS3Service, never()).deleteFile(anyString());
        verify(documentCollectionRepository, never()).delete(any(DocumentCollection.class));
        verifyNoInteractions(s);
    }

    @Test
    void deleteFileFromCollection_success_updatesCollection() {
        // Arrange
        when(s.sanitizeLogging(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        String collectionId = UUID.randomUUID().toString();
        String documentIdToRemove = UUID.randomUUID().toString();
        String storageIdToRemove = "storageIdToRemove";
        String urlToRemove = "https://s3.amazonaws.com/bucket/storageIdToRemove";

        FileEntry fileToRemove = FileEntry.builder().documentId(documentIdToRemove).storageId(storageIdToRemove).fileUrl(urlToRemove).uploadStatus(DocumentUploadState.SUCCESS.toString()).build();
        FileEntry remainingFile = FileEntry.builder().documentId(UUID.randomUUID().toString()).storageId("otherStorageId").fileUrl("https://s3.amazonaws.com/bucket/otherStorageId").uploadStatus(DocumentUploadState.SUCCESS.toString()).build();
        List<FileEntry> files = new ArrayList<>(List.of(fileToRemove, remainingFile));

        DocumentCollection collection = DocumentCollection.builder()
                .id(collectionId)
                .user(testUser)
                .files(files)
                .collectionStatus(DocumentStatus.COMPLETED)
                .build();

        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));
        doNothing().when(awsS3Service).deleteFile(urlToRemove);
        when(documentCollectionRepository.save(any(DocumentCollection.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        documentDeleteService.deleteFileFromCollection(collectionId, documentIdToRemove, testUser.getId());

        // Assert
        verify(awsS3Service).deleteFile(urlToRemove);

        ArgumentCaptor<DocumentCollection> collectionCaptor = ArgumentCaptor.forClass(DocumentCollection.class);
        verify(documentCollectionRepository).save(collectionCaptor.capture());
        DocumentCollection savedCollection = collectionCaptor.getValue();

        assertEquals(1, savedCollection.getFiles().size());
        assertFalse(savedCollection.getFiles().contains(fileToRemove));
        assertTrue(savedCollection.getFiles().contains(remainingFile));
        assertEquals(DocumentStatus.COMPLETED, savedCollection.getCollectionStatus());

        verify(s).sanitizeLogging(storageIdToRemove);
        verify(s, times(2)).sanitizeLogging(documentIdToRemove);
        verify(s).sanitizeLogging(collectionId);
    }

    @Test
    void deleteFileFromCollection_lastFileDeletesCollection() {
        // Arrange
        when(s.sanitizeLogging(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        String collectionId = UUID.randomUUID().toString();
        String documentIdToRemove = UUID.randomUUID().toString();
        String storageIdToRemove = "storageIdToRemove";
        String urlToRemove = "https://s3.amazonaws.com/bucket/storageIdToRemove";

        FileEntry fileToRemove = FileEntry.builder().documentId(documentIdToRemove).storageId(storageIdToRemove).fileUrl(urlToRemove).uploadStatus(DocumentUploadState.SUCCESS.toString()).build();
        List<FileEntry> files = new ArrayList<>(List.of(fileToRemove));

        DocumentCollection collection = DocumentCollection.builder()
                .id(collectionId)
                .user(testUser)
                .files(files)
                .collectionStatus(DocumentStatus.COMPLETED)
                .build();

        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));
        doNothing().when(awsS3Service).deleteFile(urlToRemove);
        doNothing().when(documentCollectionRepository).delete(any(DocumentCollection.class));


        // Act
        documentDeleteService.deleteFileFromCollection(collectionId, documentIdToRemove, testUser.getId());

        // Assert
        verify(awsS3Service).deleteFile(urlToRemove);
        verify(documentCollectionRepository).delete(collection);
        verify(documentCollectionRepository, never()).save(any(DocumentCollection.class));
        assertTrue(collection.getFiles().isEmpty());

        verify(s).sanitizeLogging(storageIdToRemove);
        verify(s, times(2)).sanitizeLogging(documentIdToRemove);
        verify(s).sanitizeLogging(collectionId);
    }

    @Test
    void deleteFileFromCollection_fileNotFoundInCollection() {
        // Arrange
        String collectionId = UUID.randomUUID().toString();
        String nonExistentDocumentId = UUID.randomUUID().toString();
        DocumentCollection collection = DocumentCollection.builder().id(collectionId).user(testUser).files(new ArrayList<>()).build();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                documentDeleteService.deleteFileFromCollection(collectionId, nonExistentDocumentId, testUser.getId()));

        assertEquals("File with document ID: " + nonExistentDocumentId + " not found in collection: " + collectionId, exception.getMessage());
        verify(awsS3Service, never()).deleteFile(anyString());
        verifyNoInteractions(s);
    }

    @Test
    void deleteFileFromCollection_collectionNotFound() {
        // Arrange
        String collectionId = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                documentDeleteService.deleteFileFromCollection(collectionId, documentId, testUser.getId()));

        assertEquals("Document collection not found with ID: " + collectionId, exception.getMessage());
        verifyNoInteractions(s);
    }

    @Test
    void deleteFileFromCollection_forbidden() {
        // Arrange
        String collectionId = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID().toString());


        FileEntry existingFile = FileEntry.builder().documentId(documentId).storageId("someStorageId").fileUrl("https://s3.amazonaws.com/bucket/someStorageId").build();
        DocumentCollection collection = DocumentCollection.builder()
                .id(collectionId)
                .user(anotherUser)
                .files(new ArrayList<>(List.of(existingFile)))
                .build();
        when(documentCollectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                documentDeleteService.deleteFileFromCollection(collectionId, documentId, testUser.getId()));

        assertEquals("You are not authorized to modify this document collection.", exception.getMessage());
        verify(awsS3Service, never()).deleteFile(anyString());
        verifyNoInteractions(s);
    }
}