package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.request.ReceiptFilterDto;
import com.extractor.unraveldocs.admin.dto.response.AdminReceiptDto;
import com.extractor.unraveldocs.admin.interfaces.AdminReceiptListService;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReceiptListServiceImpl implements AdminReceiptListService {

    private final ReceiptRepository receiptRepository;
    private final ResponseBuilderService responseBuilder;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<Page<AdminReceiptDto>> getReceipts(ReceiptFilterDto filter, int page, int size) {
        log.info("Fetching receipts list for admin dashboard. Page: {}, Size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Specification<Receipt> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), filter.getUserId()));
            }
            if (filter.getProvider() != null) {
                predicates.add(cb.equal(root.get("paymentProvider"), filter.getProvider()));
            }
            if (filter.getCurrency() != null) {
                predicates.add(cb.equal(root.get("currency"), filter.getCurrency()));
            }
            if (filter.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getDateFrom()));
            }
            if (filter.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getDateTo()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Receipt> receiptsPage = receiptRepository.findAll(spec, pageable);
        Page<AdminReceiptDto> dtoPage = receiptsPage.map(this::mapToDto);

        return responseBuilder.buildUserResponse(dtoPage, HttpStatus.OK, "Receipts retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<AdminReceiptDto> getReceiptDetail(String receiptId) {
        log.info("Fetching receipt details for {}", receiptId);
        
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
                
        return responseBuilder.buildUserResponse(mapToDto(receipt), HttpStatus.OK, "Receipt details retrieved successfully");
    }

    private AdminReceiptDto mapToDto(Receipt receipt) {
        AdminReceiptDto dto = AdminReceiptDto.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .paymentProvider(receipt.getPaymentProvider())
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .paymentMethod(receipt.getPaymentMethod())
                .paymentMethodDetails(receipt.getPaymentMethodDetails())
                .description(receipt.getDescription())
                .receiptUrl(receipt.getReceiptUrl())
                .paidAt(receipt.getPaidAt())
                .emailSent(receipt.isEmailSent())
                .createdAt(receipt.getCreatedAt())
                .build();
                
        if (receipt.getUser() != null) {
            dto.setUserId(receipt.getUser().getId());
            dto.setUserEmail(receipt.getUser().getEmail());
            dto.setUserFullName(receipt.getUser().getFirstName() + " " + receipt.getUser().getLastName());
        }
        
        return dto;
    }
}
