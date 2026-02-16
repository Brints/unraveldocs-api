package com.extractor.unraveldocs.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for credit transfer operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditTransferData {

    private String transferId;
    private int creditsTransferred;
    private int senderBalanceAfter;
    private String recipientEmail;
    private String recipientName;
}
