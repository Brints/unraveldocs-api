package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminReceiptActionsService {

    /**
     * Resend a receipt email for a specific receipt.
     *
     * @param receiptId The ID of the receipt to resend
     * @return Response indicating success
     */
    UnravelDocsResponse<String> resendReceiptEmail(String receiptId);
}
