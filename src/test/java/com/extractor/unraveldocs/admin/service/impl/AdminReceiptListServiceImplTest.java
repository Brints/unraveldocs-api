package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.ReceiptFilterDto;
import com.extractor.unraveldocs.admin.dto.response.AdminReceiptDto;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReceiptListServiceImplTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AdminReceiptListServiceImpl adminReceiptListService;

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
                .build();
    }

    @Test
    void getReceipts_success() {
        // Arrange
        ReceiptFilterDto filter = new ReceiptFilterDto();
        Page<Receipt> page = new PageImpl<>(List.of(mockReceipt));
        
        @SuppressWarnings("unchecked")
        Specification<Receipt> spec = any(Specification.class);
        when(receiptRepository.findAll(spec, any(Pageable.class))).thenReturn(page);
        
        when(responseBuilderService.buildUserResponse(any(), eq(HttpStatus.OK), anyString()))
                .thenAnswer(invocation -> new UnravelDocsResponse<>(
                        HttpStatus.OK.value(),
                        "success",
                        "Receipts retrieved successfully",
                        invocation.getArgument(0)
                ));

        // Act
        UnravelDocsResponse<Page<AdminReceiptDto>> response = adminReceiptListService.getReceipts(filter, 0, 10);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getData().getContent()).hasSize(1);
        
        AdminReceiptDto dto = response.getData().getContent().get(0);
        assertThat(dto.getId()).isEqualTo("rec_123");
        assertThat(dto.getUserId()).isEqualTo("usr_123");
        assertThat(dto.getUserEmail()).isEqualTo("test@unraveldocs.com");
    }

    @Test
    void getReceiptDetail_success() {
        when(receiptRepository.findById("rec_123")).thenReturn(Optional.of(mockReceipt));
        
        when(responseBuilderService.buildUserResponse(any(), eq(HttpStatus.OK), anyString()))
                .thenAnswer(invocation -> new UnravelDocsResponse<>(
                        HttpStatus.OK.value(),
                        "success",
                        "Receipt details retrieved successfully",
                        invocation.getArgument(0)
                ));

        UnravelDocsResponse<AdminReceiptDto> response = adminReceiptListService.getReceiptDetail("rec_123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getData().getId()).isEqualTo("rec_123");
        assertThat(response.getData().getPaymentProvider()).isEqualTo(PaymentProvider.STRIPE);
    }

    @Test
    void getReceiptDetail_notFound_throwsException() {
        when(receiptRepository.findById("rec_999")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminReceiptListService.getReceiptDetail("rec_999"));
        
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
