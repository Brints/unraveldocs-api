package com.extractor.unraveldocs.admin.dto.request;

import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * Filter DTO for searching and listing receipts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptFilterDto {
    private String userId;
    private PaymentProvider provider;
    private String currency;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime dateFrom;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime dateTo;
}
