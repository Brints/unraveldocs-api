package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.payment.receipt.service.ReceiptEmailService;
import com.extractor.unraveldocs.payment.receipt.service.ReceiptPdfService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReceiptActionsServiceImplTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ReceiptPdfService receiptPdfService;

    @Mock
    private ReceiptEmailService receiptEmailService;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AdminReceiptActionsServiceImpl adminReceiptActionsService;

    private User mockUser;
    private Receipt mockReceipt;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId("usr_123");
        mockUser.setEmail("test@unraveldocs.com");
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");

        mockReceipt = Receipt.builder()
                .id("rec_123")
                .receiptNumber("RC-123")
                .paymentProvider(PaymentProvider.STRIPE)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .user(mockUser)
                .emailSent(false)
                .build();
    }

    @Test
    void resendReceiptEmail_success() throws Exception {
        // Arrange
        when(receiptRepository.findById("rec_123")).thenReturn(Optional.of(mockReceipt));
        
        byte[] mockPdf = new byte[]{1, 2, 3};
        when(receiptPdfService.generateReceiptPdf(anyString(), any(ReceiptData.class))).thenReturn(mockPdf);
        
        when(responseBuilderService.buildUserResponse(any(), eq(HttpStatus.OK), anyString()))
                .thenAnswer(invocation -> new UnravelDocsResponse<>(
                        HttpStatus.OK.value(),
                        "success",
                        "Success",
                        invocation.getArgument(0)
                ));

        // Act
        UnravelDocsResponse<String> response = adminReceiptActionsService.resendReceiptEmail("rec_123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        
        ArgumentCaptor<ReceiptData> dataCaptor = ArgumentCaptor.forClass(ReceiptData.class);
        verify(receiptPdfService).generateReceiptPdf(eq("RC-123"), dataCaptor.capture());
        
        ReceiptData capturedData = dataCaptor.getValue();
        assertThat(capturedData.getCustomerEmail()).isEqualTo("test@unraveldocs.com");
        assertThat(capturedData.getAmount()).isEqualTo(new BigDecimal("100.00"));
        
        verify(receiptEmailService).sendReceiptEmail(eq("RC-123"), any(ReceiptData.class), eq(mockPdf));
        
        ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptRepository).save(receiptCaptor.capture());
        
        Receipt savedReceipt = receiptCaptor.getValue();
        assertThat(savedReceipt.isEmailSent()).isTrue();
        assertThat(savedReceipt.getEmailSentAt()).isNotNull();
    }

    @Test
    void resendReceiptEmail_userNull_throwsException() {
        mockReceipt.setUser(null);
        when(receiptRepository.findById("rec_123")).thenReturn(Optional.of(mockReceipt));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminReceiptActionsService.resendReceiptEmail("rec_123"));
                
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resendReceiptEmail_pdfGenerationFails_throwsException() throws Exception {
        when(receiptRepository.findById("rec_123")).thenReturn(Optional.of(mockReceipt));
        
        when(receiptPdfService.generateReceiptPdf(anyString(), any(ReceiptData.class)))
                .thenThrow(new RuntimeException("PDF Gen Error"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminReceiptActionsService.resendReceiptEmail("rec_123"));
                
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
