package com.extractor.unraveldocs.credit.datamodel;

import lombok.Getter;

/**
 * Enum representing types of credit transactions.
 */
@Getter
public enum CreditTransactionType {
    PURCHASE("Credit Pack Purchase"),
    DEDUCTION("OCR Page Processing"),
    REFUND("Credit Refund"),
    BONUS("Sign-up Bonus"),
    ADMIN_ADJUSTMENT("Admin Adjustment"),
    TRANSFER_SENT("Credits Sent to User"),
    TRANSFER_RECEIVED("Credits Received from User"),
    ADMIN_ALLOCATION("Admin Credit Allocation");

    private final String description;

    CreditTransactionType(String description) {
        this.description = description;
    }

}
