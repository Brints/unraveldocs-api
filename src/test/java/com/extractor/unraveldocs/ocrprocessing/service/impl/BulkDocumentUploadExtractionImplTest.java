package com.extractor.unraveldocs.ocrprocessing.service.impl;

import com.extractor.unraveldocs.messagequeuing.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionUploadData;
import com.extractor.unraveldocs.documents.datamodel.DocumentStatus;
import com.extractor.unraveldocs.documents.datamodel.DocumentUploadState;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.messagequeuing.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.messagequeuing.rabbitmq.events.EventPublisherService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.ocrprocessing.events.OcrEventMapper;
import com.extractor.unraveldocs.ocrprocessing.events.OcrRequestedEvent;
import com.extractor.unraveldocs.ocrprocessing.impl.BulkDocumentUploadExtractionImpl;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.ocrprocessing.utils.FileStorageService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.utils.imageupload.FileUploadValidationUtil;
import com.extractor.unraveldocs.utils.imageupload.FileSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkDocumentUploadExtractionImplTest {

    @Mock
    private DocumentCollectionRepository documentCollectionRepository;
    @Mock
    private OcrDataRepository ocrDataRepository;
    @Mock
    private EventPublisherService eventPublisherService;
    @Mock
    private OcrEventMapper ocrEventMapper;
    @Mock
    private SanitizeLogging s;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private BulkDocumentUploadExtractionImpl bulkDocumentUploadExtractionService;

    private User user;
    private MockMultipartFile validFile1;
    private MockMultipartFile validFile2;
    private MockMultipartFile invalidFileTypeFile;
    private MockedStatic<FileUploadValidationUtil> mockedValidationUtil;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID().toString());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }

        byte[] smallContent = "c".getBytes();
        validFile1 = new MockMultipartFile("files", "test1.png", "image/png", smallContent);
        validFile2 = new MockMultipartFile("files", "test2.jpeg", "image/jpeg", smallContent);
        invalidFileTypeFile = new MockMultipartFile("files", "fail.txt", "text/plain", smallContent);

        mockedValidationUtil = mockStatic(FileUploadValidationUtil.class);
        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateTotalFileSize(any(MultipartFile[].class))).then(invocation -> null);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
        mockedValidationUtil.close();
    }

    @Test
    void uploadDocuments_Success() {
        // Arrange
        MultipartFile[] files = {validFile1, validFile2};
        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateIndividualFile(any(MultipartFile.class))).then(invocation -> null);


        FileEntry fileEntry1 = FileEntry.builder().documentId(UUID.randomUUID().toString()).originalFileName(validFile1.getOriginalFilename()).fileUrl("url1").uploadStatus(DocumentUploadState.SUCCESS.toString()).build();
        FileEntry fileEntry2 = FileEntry.builder().documentId(UUID.randomUUID().toString()).originalFileName(validFile2.getOriginalFilename()).fileUrl("url2").uploadStatus(DocumentUploadState.SUCCESS.toString()).build();

        when(fileStorageService.handleSuccessfulFileUpload(eq(validFile1), anyString())).thenReturn(fileEntry1);
        when(fileStorageService.handleSuccessfulFileUpload(eq(validFile2), anyString())).thenReturn(fileEntry2);

        DocumentCollection savedCollection = new DocumentCollection();
        savedCollection.setId(UUID.randomUUID().toString());
        savedCollection.setFiles(List.of(fileEntry1, fileEntry2));
        savedCollection.setCollectionStatus(DocumentStatus.PROCESSING);
        when(documentCollectionRepository.saveAndFlush(any(DocumentCollection.class))).thenReturn(savedCollection);
        when(ocrEventMapper.toOcrRequestedEvent(any(FileEntry.class), anyString())).thenReturn(new OcrRequestedEvent());

        // Act
        DocumentCollectionResponse<DocumentCollectionUploadData> response = bulkDocumentUploadExtractionService.uploadDocuments(files, user);

        // Assert
        assertNotNull(response);
        assertEquals(202, response.getStatusCode());
        assertEquals("2 document(s) uploaded successfully and queued for processing. 0 failed.", response.getMessage());
        assertNotNull(response.getData().getCollectionId());
        assertEquals(DocumentStatus.PROCESSING, response.getData().getOverallStatus());
        assertEquals(2, response.getData().getFiles().size());
        assertTrue(response.getData().getFiles().stream().allMatch(f -> f.getStatus().equals(DocumentUploadState.SUCCESS.toString())));

        ArgumentCaptor<DocumentCollection> collectionCaptor = ArgumentCaptor.forClass(DocumentCollection.class);
        verify(documentCollectionRepository).saveAndFlush(collectionCaptor.capture());

        // Manually trigger afterCommit to simulate transaction completion
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertFalse(synchronizations.isEmpty());
        synchronizations.get(0).afterCommit();

        verify(ocrDataRepository, times(1)).saveAll(anyList());
        verify(eventPublisherService, times(2)).publishEvent(eq(RabbitMQQueueConfig.OCR_EVENTS_EXCHANGE), eq(RabbitMQQueueConfig.OCR_ROUTING_KEY), any(BaseEvent.class));
    }

    @Test
    void uploadDocuments_PartialSuccessWithFailures() {
        // Arrange
        MockMultipartFile storageFailFile = new MockMultipartFile("files", "storage_fail.png", "image/png", "content".getBytes());
        MultipartFile[] files = {validFile1, invalidFileTypeFile, storageFailFile};

        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateIndividualFile(validFile1)).then(invocation -> null);
        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateIndividualFile(storageFailFile)).then(invocation -> null);
        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateIndividualFile(invalidFileTypeFile))
                .thenThrow(new BadRequestException("Invalid file type"));


        FileEntry successFileEntry = FileEntry.builder().documentId(UUID.randomUUID().toString()).originalFileName(validFile1.getOriginalFilename()).fileUrl("url_success").uploadStatus(DocumentUploadState.SUCCESS.toString()).build();
        when(fileStorageService.handleSuccessfulFileUpload(eq(validFile1), anyString())).thenReturn(successFileEntry);
        when(fileStorageService.handleSuccessfulFileUpload(eq(storageFailFile), anyString())).thenThrow(new RuntimeException("Storage unavailable"));

        DocumentCollection savedCollection = new DocumentCollection();
        savedCollection.setId(UUID.randomUUID().toString());
        savedCollection.setFiles(List.of(successFileEntry));
        savedCollection.setCollectionStatus(DocumentStatus.PROCESSING);
        when(documentCollectionRepository.saveAndFlush(any(DocumentCollection.class))).thenReturn(savedCollection);
        when(ocrEventMapper.toOcrRequestedEvent(any(FileEntry.class), anyString())).thenReturn(new OcrRequestedEvent());

        // Act
        DocumentCollectionResponse<DocumentCollectionUploadData> response = bulkDocumentUploadExtractionService.uploadDocuments(files, user);

        // Assert
        assertEquals(202, response.getStatusCode());
        assertEquals("1 document(s) uploaded successfully and queued for processing. 2 failed.", response.getMessage());
        assertEquals(DocumentStatus.PROCESSING, response.getData().getOverallStatus());
        assertEquals(3, response.getData().getFiles().size());

        assertTrue(response.getData().getFiles().stream().anyMatch(f -> DocumentUploadState.SUCCESS.toString().equals(f.getStatus()) && f.getOriginalFileName().equals(validFile1.getOriginalFilename())));
        assertTrue(response.getData().getFiles().stream().anyMatch(f -> DocumentUploadState.FAILED_VALIDATION.toString().equals(f.getStatus()) && f.getOriginalFileName().equals(invalidFileTypeFile.getOriginalFilename())));
        assertTrue(response.getData().getFiles().stream().anyMatch(f -> DocumentUploadState.FAILED_STORAGE_UPLOAD.toString().equals(f.getStatus()) && f.getOriginalFileName().equals(storageFailFile.getOriginalFilename())));

        verify(documentCollectionRepository, times(1)).saveAndFlush(any(DocumentCollection.class));

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertFalse(synchronizations.isEmpty());
        synchronizations.getFirst().afterCommit();

        verify(ocrDataRepository, times(1)).saveAll(anyList());
        verify(eventPublisherService, times(1)).publishEvent(anyString(), anyString(), any(BaseEvent.class));
    }

    @Test
    void uploadDocuments_AllFailValidation() {
        // Arrange
        MockMultipartFile file2 = new MockMultipartFile("files", "fail2.exe", "application/octet-stream", "content".getBytes());
        MultipartFile[] files = {invalidFileTypeFile, file2};
        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateIndividualFile(eq(invalidFileTypeFile)))
                .thenThrow(new BadRequestException("Invalid file type"));
        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateIndividualFile(eq(file2)))
                .thenThrow(new BadRequestException("Invalid file type"));

        // Act
        DocumentCollectionResponse<DocumentCollectionUploadData> response = bulkDocumentUploadExtractionService.uploadDocuments(files, user);

        // Assert
        assertEquals(202, response.getStatusCode());
        assertEquals("All document uploads failed.", response.getMessage());
        assertNull(response.getData().getCollectionId());
        assertEquals(DocumentStatus.FAILED_UPLOAD, response.getData().getOverallStatus());
        assertEquals(2, response.getData().getFiles().size());
        assertTrue(response.getData().getFiles().stream().allMatch(f -> f.getStatus().equals(DocumentUploadState.FAILED_VALIDATION.toString())));

        verify(documentCollectionRepository, never()).save(any());
        verify(ocrDataRepository, never()).saveAll(any());
        verify(fileStorageService, never()).handleSuccessfulFileUpload(any(), any());
        verify(eventPublisherService, never()).publishEvent(anyString(), anyString(), any(BaseEvent.class));
    }

    @Test
    void uploadDocuments_failure_totalFileSizeExceedsLimit() {
        // Arrange
        MultipartFile[] files = {validFile1};
        String expectedMessage = FileSize.getFileSizeLimitMessage(true);
        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateTotalFileSize(files))
                .thenThrow(new BadRequestException(expectedMessage));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> bulkDocumentUploadExtractionService
                .uploadDocuments(files, user));

        assertEquals(expectedMessage, exception.getMessage());
        verify(documentCollectionRepository, never()).save(any(DocumentCollection.class));
    }

    @Test
    void uploadDocuments_failure_individualFileSizeExceedsLimit() {
        // Arrange
        MockMultipartFile largeFile = new MockMultipartFile("files", "large.png", "image/png", "large content".getBytes());
        MultipartFile[] files = {largeFile};
        mockedValidationUtil.when(() -> FileUploadValidationUtil.validateIndividualFile(largeFile))
                .thenThrow(new BadRequestException("File size exceeds limit"));

        // Act
        DocumentCollectionResponse<DocumentCollectionUploadData> response = bulkDocumentUploadExtractionService.uploadDocuments(files, user);

        // Assert
        assertNotNull(response);
        assertEquals(202, response.getStatusCode());
        assertEquals("All document uploads failed.", response.getMessage());
        assertNull(response.getData().getCollectionId());
        assertEquals(DocumentStatus.FAILED_UPLOAD, response.getData().getOverallStatus());
        assertEquals(1, response.getData().getFiles().size());
        assertEquals(DocumentUploadState.FAILED_VALIDATION.toString(), response.getData().getFiles().get(0).getStatus());

        verify(documentCollectionRepository, never()).save(any(DocumentCollection.class));
    }
}