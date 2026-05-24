package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.request.ReceiptFilterDto;
import com.extractor.unraveldocs.admin.dto.response.AdminReceiptDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import org.springframework.data.domain.Page;

public interface AdminReceiptListService {

    /**
     * Get a paginated list of receipts with optional filters.
     *
     * @param filter The filter criteria
     * @param page   Page number
     * @param size   Page size
     * @return Paginated response containing receipts
     */
    UnravelDocsResponse<Page<AdminReceiptDto>> getReceipts(ReceiptFilterDto filter, int page, int size);

    /**
     * Get details of a specific receipt.
     *
     * @param receiptId The ID of the receipt
     * @return Response containing the receipt details
     */
    UnravelDocsResponse<AdminReceiptDto> getReceiptDetail(String receiptId);
}
